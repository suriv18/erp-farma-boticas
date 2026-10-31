# DAT-FAR-010 — Diseño detallado del surtido diverso

**Estado:** especificación documental cerrada para implementación. **Fecha:** 2026-09-15.
**Decisión:** [ADR-011](../05-arquitectura/adr/ADR-011-surtido-trazabilidad-admisibilidad.md).

## 1. Convenciones y fronteras

Los nombres de esta sección designan tablas objetivo; no están creadas en V001–V017. PK BIGINT IDENTITY y UUID público para recursos expuestos. Toda entidad nueva es tenant-scoped y las FKs incluyen tenant_id. Campos comunes: id, tenant_id, created_at, created_by; los maestros incluyen uuid_publico y es_activo. Las versiones publicadas son inmutables; correcciones crean una nueva versión. La auditoría conserva actor, razón y correlation_id.

Los valores de régimen son un catálogo del proyecto: MEDICAMENTO, DIETETICO, COSMETICO, DISPOSITIVO_MEDICO, OTRO_SANITARIO, ALIMENTO y OTRO_ARTICULO. No sustituyen la clasificación de la autoridad. OTRO_ARTICULO requiere justificación; no sirve para evadir una clasificación faltante.

BC-CAT mantiene clasificación, evidencias y políticas. BC-INV mantiene existencias y trazabilidad. BC-RET coordina la confirmación con reglas por línea. La autorización de un producto y la admisibilidad en un local son decisiones separadas.

## 2. Fuente canónica de autorización sanitaria

| Entidad objetivo | Campos específicos y tipos | Garantías |
|---|---|---|
| sch_catalogo.expediente_sanitario | autoridad_codigo VARCHAR(40), tipo_documento VARCHAR(40), numero_documento VARCHAR(150) nullable, clave_sin_registro UUID nullable | UNIQUE tenant/autoridad/tipo/número para documentos identificados; para NO_APLICA usar clave propia no nula; nunca número ficticio |
| sch_catalogo.expediente_sanitario_version | expediente_id BIGINT, numero_version INTEGER, regimen_codigo VARCHAR(40), titular TEXT, vigente_desde DATE nullable, vigente_hasta DATE nullable, estado VARCHAR(30), fuente_uri TEXT, evidencia_uri TEXT, evidencia_sha256 CHAR(64), verificado_at TIMESTAMPTZ, revisable_hasta TIMESTAMPTZ, verificado_por BIGINT, justificacion TEXT | UNIQUE tenant/expediente/versión; FK al expediente y al usuario del tenant; estado VERIFICADO, OBSERVADO o REVOCADO; fecha fin no anterior al inicio |
| sch_catalogo.sku_expediente | sku_id BIGINT, expediente_id BIGINT, finalidad VARCHAR(40) | UNIQUE tenant/SKU/expediente/finalidad; varias evidencias por SKU cuando corresponda |

NO_APLICA significa que el documento no es exigible según la revisión conservada; no significa que el producto esté autorizado en todo local. fuente_uri identifica la fuente y evidencia_uri una copia controlada fuera de la BD. No insertar datos personales o credenciales en URLs.

La revisión crea la siguiente versión del expediente bajo bloqueo del encabezado; su número es monotónico y no lo elige el cliente. La evaluación usa la última revisión publicada, nunca una versión antigua elegida para evitar una revocación. verificado_at lo asigna el servidor. Una nueva revisión entra en vigor al publicarse y participa en la coordinación de surtido_control.

Mantener un solo encabezado por documento dentro del tenant, compartido por SKU compatibles. Las fechas pueden ser nulas únicamente si su no aplicabilidad está justificada. La política de revisión establece revisable_hasta TIMESTAMPTZ obligatorio para no reutilizar una verificación indefinidamente; su valor se asigna al revisar cada caso, no se fija como plazo legal universal.

numero_registro y tipo_registro de producto_regulado siguen como referencia global importada. No los reescribe un tenant. Si difieren del expediente, generar observación y bloquear la activación hasta revisión. codigo_nso del SKU se ofrece como proyección del expediente seleccionado; durante transición se compara y después se retira su escritura. Una API no acepta dos fuentes editables del mismo dato.

## 3. Clasificación y versiones de política

| Entidad objetivo | Campos específicos | Garantías |
|---|---|---|
| sch_catalogo.sku_politica | sku_id BIGINT, version INTEGER, regimen_codigo VARCHAR(40), exige_lote BOOLEAN, exige_vencimiento BOOLEAN, exige_serie BOOLEAN, estado VARCHAR(20), efectiva_desde TIMESTAMPTZ | UNIQUE tenant/SKU/versión; CHECK vencimiento implica lote; estado BORRADOR o PUBLICADA |
| sch_catalogo.sku_politica_actual | sku_id BIGINT, politica_id BIGINT, version_lock BIGINT | PK tenant/SKU; FK tenant/SKU/política; actualización compare-and-swap; un único puntero actual |

El expediente determina el sustento sanitario; la política establece cómo operar el SKU. Cambiar de política con stock o documentos abiertos exige un plan de transición: no retirar lote/serie de existencias históricas. La aplicación usa la política vigente para nuevas operaciones y conserva la anterior en documentos cerrados.

| exige_lote | exige_vencimiento | exige_serie | Operación |
|---|---|---|---|
| false | false | false | Existencias por SKU/ubicación/estado, sin lote |
| true | false | false | Lote sin caducidad obligatoria |
| true | true | false | Lote con vencimiento y selección FEFO donde proceda |
| false | false | true | Serie por unidad sin lote |
| true | false | true | Serie y lote, sin vencimiento obligatorio |
| true | true | true | Serie y lote con vencimiento |

Toda combinación con vencimiento=true y lote=false se rechaza. La elección por SKU se revisa con su evidencia; la tabla no asigna perfiles automáticamente por rubro.

## 4. Admisibilidad por establecimiento

| Entidad objetivo | Campos específicos | Garantías |
|---|---|---|
| sch_catalogo.regla_surtido_version | sku_id BIGINT, version INTEGER, empresa_id BIGINT nullable, establecimiento_id BIGINT nullable, canal VARCHAR(30), resultado VARCHAR(20), motivo_codigo VARCHAR(60), vigente_desde TIMESTAMPTZ, vigente_hasta TIMESTAMPTZ, fuente_uri TEXT, evidencia_version_id BIGINT nullable, autor_usuario_id BIGINT | Intervalo [desde,hasta), fin obligatorio; resultado PERMITIDO, PROHIBIDO o PENDIENTE; FK contextual de local; establecimiento implica empresa; canal explícito |
| sch_catalogo.surtido_control | tenant_id BIGINT | Fila de coordinación para evaluación y publicación de bloqueos; también puede soportar una versión monotónica |
| sch_venta.venta_linea_decision | venta_linea_id BIGINT, politica_id BIGINT, evaluado_at TIMESTAMPTZ, resultado VARCHAR(20), contexto JSONB, decision_uuid UUID | UNIQUE tenant/línea para decisión final; solo insertar al confirmar |
| sch_venta.venta_linea_decision_regla | decision_id BIGINT, regla_id BIGINT | UNIQUE tenant/decisión/regla; guarda todas las reglas aplicadas |
| sch_venta.venta_linea_decision_evidencia | decision_id BIGINT, evidencia_version_id BIGINT | UNIQUE tenant/decisión/evidencia; historial inmutable |

Cada regla tiene un encabezado regla_surtido con SKU, ámbito y canal estables, y un puntero regla_surtido_actual a su versión publicada. regla_surtido_version incluye regla_id y UNIQUE tenant/regla/versión. La publicación actualiza el puntero bajo surtido_control; una corrección o revocación publica una versión nueva. La evaluación solo usa versiones actuales efectivas, mientras las decisiones históricas conservan sus referencias anteriores. No se permite una publicación futura que deje un intervalo sin regla por sustituir anticipadamente el puntero: la activación se realiza al llegar efectiva_desde, con la misma coordinación transaccional.

El canal inicial habilitado es POS, con evaluación online. Otros canales requieren la misma revisión y activación explícita, no heredan autorización del POS.

Algoritmo determinista:

1. Validar tenant, empresa/local, usuario, SKU activo, política publicada y estado operativo del establecimiento.
2. Reunir reglas vigentes de ese SKU/canal en ámbitos cadena, empresa y local. Rechazar intervalos solapados dentro del mismo ámbito al publicar.
3. Un PROHIBIDO aplicable bloquea, aunque exista un PERMITIDO más específico. Un PENDIENTE aplicable exige revisión. Si no hay reglas aplicables o falta la cobertura requerida, devolver PENDIENTE. Un permiso RBAC no omite esta regla.
4. Si no hay bloqueos ni pendientes y hay al menos un PERMITIDO aplicable, verificar todas las evidencias exigibles, revisable_hasta y política del SKU. Solo entonces permitir.
5. La consulta es informativa. Al confirmar se vuelve a evaluar dentro de la transacción; se preservan IDs de reglas/evidencias y no solo un texto resumen.

Un nuevo bloqueo no puede entrar entre evaluación y commit sin un orden definido. En el diseño inicial, la confirmación toma bloqueo compartido de surtido_control y las publicaciones/revocaciones toman bloqueo exclusivo de esa fila antes de escribir. Todos los escritores usan el mismo orden: control de tenant → política/SKU → posiciones ordenadas por ID. Un bloqueo confirmado primero impide ventas posteriores; una venta confirmada antes permanece histórica. Optimizar la granularidad requiere medir contención y conservar esta garantía.

## 5. Variantes y series

familia_comercial: tenant, uuid_publico, codigo único, nombre y atributos_definicion JSONB. La definición fija atributos permitidos, tipos, obligatoriedad y valores. sku_comercial añade familia_id nullable y atributos_canonicos JSONB. La normalización utiliza valores de catálogo, tipos estables y claves ordenadas; la igualdad real del JSONB, junto con tenant/familia y SKU activo, impide duplicados. Rechazar atributos desconocidos y valores ambiguos antes de persistir. Si un SKU declara atributos de variante, familia_id es obligatorio; un SKU sin familia solo acepta atributos de variante vacíos. Las descripciones traducidas no son parte de la identidad.

serie_inventario: tenant, uuid_publico, sku_id, lote_id nullable, posicion_id, numero_serie VARCHAR(150), estado (DISPONIBLE, RESERVADA, VENDIDA, CUARENTENA, BAJA). UNIQUE tenant/SKU/serie incluso después de baja: una serie física no se recicla. Conservar la representación del fabricante; no cambiar mayúsculas o ceros iniciales sin una regla explícita. FK tenant/SKU/lote cuando exista lote y FK contextual a posición.

movimiento_inventario_serie relaciona movimiento y serie; UNIQUE tenant/movimiento/serie. venta_linea_serie y devolucion_linea_serie relacionan documentos y unidades. Cada movimiento serializado tiene cantidad entera igual al número de series. Sumas de series y saldos de posición se actualizan en la misma transacción. Una unidad vendida no puede venderse otra vez; su devolución entra a CUARENTENA hasta disposición. Para artículos sin serie estas relaciones están vacías.

## 6. Cambios sobre inventario existente

- lote.fecha_vencimiento pasa a nullable; fechas presentes deben mantener orden fabricación ≤ vencimiento.
- posicion_inventario.lote_id y movimiento_inventario.lote_id pasan a nullable. No usar ID cero ni un lote genérico para indicar ausencia.
- Añadir UNIQUE tenant/SKU/id en lote y sustituir FKs separadas por FK tenant/SKU/lote en todas las relaciones con ambos campos. Con lote nulo, seguir verificando obligatoriamente la FK al SKU.
- La clave natural de posición usa tenant, empresa, establecimiento, almacén, ubicación, SKU, lote y estado con NULLS NOT DISTINCT para los opcionales. Inventariar y conciliar duplicados antes de crearla.
- Recalcular la unicidad de lote cuando su fecha sea nula; mantener número de lote real y evitar duplicados activos por tenant/SKU/número/fecha con NULLS NOT DISTINCT.
- La aplicación y la capa de persistencia controlan los flags contra la versión publicada. Los CHECK no consultan otras tablas: esas reglas se implementan mediante comandos transaccionales y triggers que usan la misma coordinación. El rol de aplicación no tiene escritura alternativa que omita esos controles.

### Propagación por flujo

| Flujo | Comportamiento definido |
|---|---|
| Recepción | Validar política; exigir los datos aplicables; crear lote/series solo cuando existan realmente |
| Reserva | Reservar posición sin lote o lotes concretos según política; reservar series explícitas para serializados |
| Transferencia | Despacho y recepción conservan SKU/lote/series; diferencias son hechos nuevos, no cambios del despacho |
| Conteo/ajuste | Contar por la misma clave natural; para series conciliar unidades concretas; ajuste autorizado y trazable |
| Venta | Línea referencia SKU; venta_linea_lote solo para cantidades con lote, venta_linea_serie solo para serializadas; suma asignada igual a cantidad donde corresponda |
| Dispensación/controlados | Mantener controles farmacéuticos; no relajar FKs o datos de sus tablas por facilitar artículos retail |
| Devolución | Referir línea original y trazabilidad efectiva; retorno a estado no vendible hasta disposición, sin reponer automáticamente |
| Recall | Bloquear por SKU o lote según alcance; la admisibilidad no omite bloqueos de inventario |

## 7. Migración e implementación

La especificación modifica un modelo objetivo, no el recuento actual de 109 tablas. La secuencia exacta, las garantías de datos y las pruebas de salida están en el [plan de migración](../11-devops/10-migracion-surtido.md) y el [plan de validación](../10-pruebas/03-plan-validacion-surtido.md). No queda pendiente elegir entre lotes ficticios y opcionales: la decisión es opcionalidad real con políticas verificables.
