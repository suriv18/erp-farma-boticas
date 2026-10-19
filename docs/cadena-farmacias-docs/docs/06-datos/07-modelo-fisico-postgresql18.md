# DAT-FAR-007 — Modelo Físico PostgreSQL 18

**Versión:** 0.4  
**Estado:** sincronizado documentalmente con el SQL original y las migraciones el 2026-09-15; pendiente de ejecución real sobre PostgreSQL 18.x.

## 1. Alcance

Implementa el **Core Central**. El Store Edge mantiene su persistencia física pendiente de ADR; el Core sí contiene tablas de Outbox/Inbox/checkpoint necesarias para sincronización.

## 2. Esquemas

```text
sch_admin           tenant
sch_organizacion    empresas, establecimientos, almacenes, profesionales
sch_catalogo        productos regulados, rubros, categorías, marcas, SKU
sch_abastecimiento  proveedores, solicitudes, órdenes, recepción
sch_inventario      lotes, posiciones, kardex, reservas, transferencias, conteos
sch_precio          listas, versiones, promociones
sch_venta           clientes, cajas, ventas, pagos, devoluciones
sch_dispensacion    prescripción, dispensación, controlados
sch_facturacion     comprobantes, envíos, notas de crédito
sch_vigilancia      recall, farmacovigilancia
sch_finanzas        tesorería, CxP, CxC, posting, Observatorio
sch_seguridad       IAM, RBAC, ámbitos, sesiones
sch_auditoria       auditoría y acceso a datos
sch_integracion     servicios, outbox, inbox, checkpoints
sch_app             menús, notificaciones, flags, versiones
```

No se adopta `1 Bounded Context = 1 schema`.

## 3. Convenciones PostgreSQL 18

```sql
id BIGINT GENERATED ALWAYS AS IDENTITY
uuid_publico UUID NOT NULL DEFAULT uuidv7()
```

Además:

- `CITEXT` para identificadores textuales case-insensitive donde aporta valor;
- `DATERANGE` + GiST para vigencias versionadas;
- `UNIQUE NULLS NOT DISTINCT` cuando `NULL` debe representar un mismo valor lógico;
- índices parciales para estados activos/pendientes;
- generated columns para disponibilidad/saldos/diferencias;
- JSONB reservado para snapshots/reglas/metadata, no para sustituir entidades núcleo.

## 4. Migraciones

| Migración | Área |
|---|---|
| V001 | Base, schemas, extensiones, tenant |
| V002 | Organización, establecimientos, almacenes, terminales, profesionales |
| V003 | Catálogo regulatorio, rubros comerciales, categorías/marcas y SKU |
| V004 | Proveedores, compras y recepción |
| V005 | Lotes, inventario, kardex, reservas, transferencias, conteos |
| V006 | Precios y promociones |
| V007 | Clientes, caja, ventas, pagos y devoluciones |
| V008 | Prescripción y dispensación |
| V009 | Controlados |
| V010 | Fiscal/CPE |
| V011 | Recall/FVG |
| V012 | ERP financiero/Observatorio |
| V013 | IAM/RBAC/ámbitos/sesiones; nueve FKs de compras/ventas hacia usuario |
| V014 | Auditoría |
| V015 | Integraciones/Outbox/Inbox |
| V016 | Navegación dinámica RBAC |
| V017 | Notificaciones/feature flags/versiones |

## 5. Enriquecimiento aplicado

### Organización

Empresa/establecimiento incluyen datos fiscales, contacto, geolocalización, venta online/delivery, perfil `ONLINE/STORE_EDGE` y zona horaria. Almacenes incorporan capacidades de lote, vencimiento, venta/despacho y control de temperatura.

### Producto regulado y SKU

`producto_regulado` conserva campos regulatorios como registro sanitario, condición de venta, forma, vía, ATC, titular, fabricante, importador, origen/tipo de liberación y controlados. `sku_comercial` conserva categoría, marca, dimensiones, peso, fraccionamiento, IGV, políticas operativas de lote/vencimiento y stock objetivo.

### Inventario

`lote` identifica trazabilidad; `posicion_inventario` conserva balance por ubicación/lote/estado y genera `cantidad_disponible`. `movimiento_inventario` conserva naturaleza, costo y stock antes/después.

### Venta

`cliente` y `cliente_consentimiento` están separados: una compra no implica consentimiento de marketing. Una `venta_linea` puede consumir N lotes mediante `venta_linea_lote`. Los pagos conservan referencias externas sin almacenar datos sensibles de tarjeta.

### Dispensación

Prescripción, dispensación y venta siguen separadas. `dispensacion_linea_lote` preserva los lotes efectivamente entregados.

### Fiscal

El CPE conserva snapshot del adquirente, totales tributarios, referencias XML/PDF/CDR y ciclo de envío; la nota de crédito continúa separada de la devolución física.

### Seguridad/UI

RBAC se modela como módulo → permiso → rol → ámbito. `sch_app.menu_navegacion` solo controla visibilidad/navegación; el endpoint continúa autorizando cada operación.

## 6. Decisiones preservadas

- Producto regulado ≠ SKU.
- Lote ≠ stock.
- Venta ≠ dispensación ≠ CPE.
- Devolución ≠ reingreso automático.
- Menú visible ≠ autorización.
- Evento/integración reintentado debe ser idempotente.
- datos PCI/secretos no se almacenan en payloads/logs crudos.

## 7. Diferidos

RRHH completo, CRM/CMR avanzado, e-commerce/delivery completo, contabilidad general/libro mayor y autenticación tecnológica concreta siguen fuera del baseline definitivo hasta cerrar RF/ADR.

## 8. Entregables

- [`database/migrations/`](../../database/migrations/)
- [`database/cadena_farmacias_postgresql18.sql`](../../database/cadena_farmacias_postgresql18.sql)
- [`08-consolidacion-v1-v2-campos.md`](08-consolidacion-v1-v2-campos.md)
- [`database/propuestas/rls_multitenancy.sql`](../../database/propuestas/rls_multitenancy.sql)

## 9. Fuente y estado de adecuación

El SQL original es la fuente del modelo: [cadena_farmacias_postgresql18.sql](../../database/cadena_farmacias_postgresql18.sql). V001–V017 son su distribución modular con ajustes de ejecución documentados en [database/README](../../database/README.md): nueve FKs diferidas a V013, UNIQUE de establecimiento, tres índices NULLS NOT DISTINCT y cinco ON CONFLICT de seeds.

Recuento: 15 esquemas, 109 tablas, 228 FKs y 137 índices explícitos. Ejecutar la línea base en orden sobre base vacía; para instalaciones que ya aplicaron versiones, preparar cambios incrementales y no reescribir su historial.

El catálogo añade rubro_comercial, rubro_id, codigo_nso y atributos_variante. REGULADO exige producto_regulado_id; NO_REGULADO permite omitirlo, pero el CHECK actual no impide que lo incluya. La clasificación sanitaria de un cosmético o dietético no se decide con ese flag.

El soporte de surtido diverso es parcial: inventario exige lote_id y el lote exige fecha_vencimiento, aunque el SKU permita desactivarlos. Ver [evaluación y modelo objetivo](09-adecuacion-surtido-multiproducto-peru.md). Estos campos propuestos aún no cambian el esquema.

## 10. Especificación del destino

[DAT-FAR-010](10-diseno-detallado-surtido.md) fija las entidades, campos e invariantes que implementará la evolución. No modifica el recuento de tablas actuales ni convierte el plan en una migración ejecutada.
