# DAT-FAR-009 — Adecuación a boticas y farmacias con surtido diverso en Perú

**Revisión:** 2026-09-15. **Estado:** análisis y diseño documental completados; cambios SQL pendientes de implementación.

## 1. Resultado

El sistema tiene una base comercial reutilizable para medicamentos y otros artículos, pero su adaptación es **parcial**. El catálogo contiene rubros, variantes y SKU sin producto regulado; el inventario exige lote y vencimiento. Faltan controles persistentes de admisibilidad por establecimiento y una ficha sanitaria que diferencie RS, NSO y autoridad competente.

Esta revisión contrasta el SQL original y V001–V017 con fuentes oficiales peruanas y sitios comerciales. Las reglas de implementación se identifican como DOM/TEC; no se presentan como texto de la norma. No se ha ejecutado el DDL ni auditado el funcionamiento interno de las cadenas observadas.

## 2. Evidencia de operación comercial en Perú

Inkafarma presenta vitaminas, dermocosmética y cuidado personal; Mifarma muestra cuidado del bebé, cuidado personal, promociones y búsqueda por marca/producto. Son ejemplos de surtido y experiencia comercial de cadenas peruanas, no prueba de que todo local disponga de cada artículo. [Inkafarma](https://inkafarma.pe/), [Mifarma](https://www.mifarma.com.pe/) (MKT, consulta 2026-09-15).

Mifarma también publica un flujo de retiro en botica. Para el proyecto esto justifica separar catálogo, disponibilidad y local de entrega. No demuestra qué ERP, esquema SQL o política de stock utilizan esas empresas. [Retiro en botica](https://www.mifarma.com.pe/atencion-clientes/retiro-en-botica) (MKT).

## 3. Qué categorías debe distinguir el catálogo

| Familia comercial | Tratamiento documentado | Implicación de diseño |
|---|---|---|
| Medicamentos | Verificar ficha y condición autorizada | Receta y dispensación según producto; lote y trazabilidad |
| Vitaminas y suplementos | El nombre comercial no determina el régimen; DIGEMID incluye dietéticos con vitaminas/minerales entre productos farmacéuticos | Registrar clasificación y evidencia de cada presentación; no marcar todo suplemento NO_REGULADO |
| Cosméticos y cuidado personal | Cosméticos sujetos a NSO; otros sanitarios tienen el trámite que corresponda | Ficha sanitaria diferenciada; no exigir atributos propios de medicamentos |
| Pañales, higiene y artículos para bebé | No forman una categoría jurídica única | Evaluación por artículo y uso previsto |
| Dispositivos y material médico | Identificación y régimen según finalidad del fabricante | Considerar lote, serie y evidencia aplicable |
| Alimentos | La RM 734 exceptúa agua, bebidas con electrolitos y alimentos para regímenes especiales de su prohibición general | No habilitar alimentos ordinarios por pertenecer al rubro Nutrición |
| Prendas y calzado | La RM 734 contempla excepciones neonatal y hospitalaria desechable | No habilitar moda general ni toda ropa infantil; validar excepción por SKU |

Fuentes sanitarias: [DIGEMID: dietéticos, preguntas frecuentes](https://www.digemid.minsa.gob.pe/webDigemid/preguntas-frecuentes/), [productos sanitarios](https://www.digemid.minsa.gob.pe/webDigemid/registro-sanitario/productos-sanitarios/), [NSO de cosméticos](https://www.gob.pe/22921-solicitar-la-notificacion-sanitaria-obligatoria-nso-para-productos-cosmeticos), [registro sanitario y familias](https://www.digemid.minsa.gob.pe/webDigemid/registro-sanitario/).

Para alimentos sujetos al régimen correspondiente, verificar su registro en la [consulta de DIGESA](https://consultas-digesa.minsa.gob.pe/ConsultaWebRS/Consultas/Consulta_Registro_Sanitario.aspx). Tener registro sanitario no demuestra por sí solo que el artículo pueda venderse en una botica.

### Alcance de la RM 734-2025/MINSA

Se consultaron la [resolución publicada el 31-10-2025](https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2025/PERUANO_RM_734-2025.pdf) y su [anexo publicado el 02-11-2025](https://www.digemid.minsa.gob.pe/Archivos/Normatividad/2025/ANEXO_RM_734-2025.pdf). La resolución reemplaza el listado de 2015. No se encontró una modificatoria posterior en la búsqueda realizada; debe revalidarse antes del lanzamiento.

Las excepciones de prendas no autorizan uniformes reutilizables en general. El anexo también restringe servicios; incorporar cosméticos al surtido no habilita un salón de belleza. La publicación comercial de una cadena no sustituye la comprobación normativa. La prepublicación RM 419-2025 no se usa como listado definitivo.

## 4. Evaluación del SQL actual

| Capacidad | Evidencia en el DDL | Estado / brecha |
|---|---|---|
| Rubros por cadena | rubro_comercial, tenant_id, es_farmaceutico; sku_comercial.rubro_id | Disponible; es_farmaceutico no equivale a clasificación sanitaria |
| Marca, categoría, código de barras | marca, categoria_producto, sku_codigo_barra | Disponible; rubro, categoría y marca son opcionales |
| Variantes | atributos_variante JSONB, código y precio por SKU | Parcial; faltan validación de talla/color/contenido y prevención de variantes duplicadas |
| SKU sin ficha regulatoria | tipo_sku REGULADO/NO_REGULADO, producto_regulado_id nullable | Disponible estructuralmente; NO_REGULADO no significa venta legal automática |
| Productos regulados diversos | tipo_producto/tipo_registro/numero_registro; atributos farmacéuticos opcionales | Flexible, pero sin catálogo controlado de regímenes/autoridades |
| NSO | codigo_nso en SKU; tipo_registro y numero_registro en producto_regulado | Dos ubicaciones potenciales: falta definir fuente canónica y consistencia |
| Stock sin lote | requiere_lote en SKU; lote_id NOT NULL en posición y movimiento | **No soportado de extremo a extremo** |
| Lote sin caducidad | requiere_vencimiento en SKU; lote.fecha_vencimiento NOT NULL | **No soportado** |
| Coherencia SKU/lote | FKs separadas por tenant hacia SKU y lote | No aseguran que el lote sea del mismo SKU; completar garantía |
| Series individuales | No existe entidad de serie/unidad individual | Pendiente para artículos que la necesiten |
| Venta combinada | venta_linea referencia SKU; dispensacion_linea_id opcional | Base disponible; falta verificar reglas por línea en backend y pruebas |
| Admisibilidad por local y vigencia | estado comercial/regulatorio y tipo de establecimiento | No existe decisión versionada SKU–establecimiento con evidencia |
| Devoluciones | devolución comercial y disposición separadas | Definir política por familia, estado y trazabilidad, sin reposición automática |

Los flags del SKU no cambian por sí solos los NOT NULL del inventario. No resolver esta brecha inventando lotes o fechas lejanas de vencimiento.

## 5. Modelo objetivo definido (DOM/TEC)

Separar cuatro decisiones:

1. **Rubro comercial:** organización del surtido, por ejemplo Cuidado personal o Nutrición.
2. **Régimen sanitario:** medicamento, dietético, cosmético, dispositivo, alimento u otro régimen verificado.
3. **Admisibilidad:** resultado por SKU, tipo/local, canal y fecha, con fuente y evidencia.
4. **Trazabilidad:** sin lote, lote sin vencimiento, lote con vencimiento o serie, según producto.

Los valores, relaciones y controles del diseño se fijan en [DAT-FAR-010](10-diseno-detallado-surtido.md), bajo [ADR-011](../05-arquitectura/adr/ADR-011-surtido-trazabilidad-admisibilidad.md). No son enums ya implementados en el SQL actual.

Se define un expediente sanitario por tenant y documento, con versiones inmutables y vínculos a SKU. La NSO del SKU es una proyección de lectura. Los campos, la revisión de no aplicabilidad y el tratamiento de discrepancias constan en DAT-FAR-010.

El rubro y el régimen no deben deducirse de palabras del nombre ni de una marca. Cada talla/color/presentación vendible tiene SKU identificable. Los cambios de clasificación o autorización preservan la historia de ventas.

## 6. Operación objetivo

Alta/importación → clasificación y revisión de evidencia → admisibilidad por local/canal → configuración de trazabilidad → recepción/stock → precio → venta por línea → devolución y disposición según familia.

Una venta puede incluir un cosmético y un medicamento. El cobro comparte cabecera; solo la línea que lo requiera activa receta/dispensación. Revalidar admisibilidad y bloqueo al confirmar, aunque la UI o el catálogo offline tengan una versión anterior. ADR-011 fija confirmación online para la activación inicial del nuevo surtido; ante desconexión se conserva un borrador sin confirmar. La consulta de vendibilidad no sustituye la validación al confirmar.

## 7. Diseño concluido y trabajo de ejecución

El diseño de inventario opcional, integridad SKU/lote, ficha canónica, políticas, variantes y series está cerrado en DAT-FAR-010. La implementación sigue el [plan de migración](../11-devops/10-migracion-surtido.md) y debe superar el [plan de pruebas](../10-pruebas/03-plan-validacion-surtido.md).

La evaluación de la sección 4 describe el SQL actual, no una lista de decisiones documentales sin resolver. Ver el [cierre documental](../12-roadmap/07-cierre-documental-surtido.md).
