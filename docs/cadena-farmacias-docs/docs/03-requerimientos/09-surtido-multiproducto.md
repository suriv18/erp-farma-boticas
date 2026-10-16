# REQ-FAR-MULTI — Surtido diverso: requisitos, trazabilidad y aceptación

**Fecha:** 2026-09-15. **Estado:** diseño documental definido; implementación y ejecución de pruebas pendientes.

Fuente: [análisis peruano y brechas del SQL](../06-datos/09-adecuacion-surtido-multiproducto-peru.md). El alcance incluye vitaminas, cosméticos y otros artículos admisibles; la clasificación comercial no concede autorización sanitaria.

## Trazabilidad

Todos los requisitos usan CU-CAT-002. La prueba de aceptación está en el catálogo de CA con prefijo CA- seguido del RF correspondiente.

| RF | RN | Dominio / dato | API definida | Prueba |
|---|---|---|---|---|
| RF-CAT-019 | RN-CAT-005 | BC-CAT / rubro y régimen | /skus | Vitamina y cosmético no clasificados automáticamente como NO_REGULADO |
| RF-CAT-020 | RN-CAT-006 | BC-CAT / expediente y versiones (DAT-FAR-010) | /skus/{id}/evidencias-sanitarias | Evidencia ausente bloquea publicación |
| RF-CAT-021 | RN-CAT-007 | BC-CAT / reglas de surtido (DAT-FAR-010) | /skus/{id}/vendibilidad | Prenda general rechazada; excepción revisada por SKU |
| RF-CAT-022 | RN-CAT-008 | BC-CAT / atributos_variante | /skus | Tallas distintas con stock independiente |
| RF-INV-023 | RN-INV-009 | BC-INV / posición y kardex (DAT-FAR-010) | /recepciones-compra | Artículo sin lote no usa lote ficticio |
| RF-INV-024 | RN-INV-009 | BC-INV / lote (DAT-FAR-010) | /lotes | Fecha opcional o exigida según política |
| RF-POS-023 | RN-POS-010 | BC-RET / venta_linea | /ventas | Venta combinada con receta solo donde aplique |
| RF-CAT-023 | RN-CAT-008 | BC-CAT / historial definido en DAT-FAR-010 | /skus/{id}/vendibilidad | Cambio de política no altera venta cerrada |
| RF-INV-025 | RN-INV-010 | BC-INV / series y asignaciones de DAT-FAR-010 | /ventas, /transferencias, /devoluciones | Una unidad no se vende dos veces |

## Gate de implementación

No declarar listo el surtido diverso hasta superar estos escenarios con persistencia real, autorización de backend y recepción/venta/devolución completas. Mantener casos negativos de cruces de tenant y de lote perteneciente a otro SKU. Las series y el límite de confirmación online se especifican en [ADR-011](../05-arquitectura/adr/ADR-011-surtido-trazabilidad-admisibilidad.md). La continuidad offline del nuevo surtido queda fuera de la activación inicial; no se presupone disponible.

## Especificaciones cerradas

- [Diccionario, invariantes y algoritmo](../06-datos/10-diseno-detallado-surtido.md).
- [Contrato de operaciones, campos y errores](../07-api/08-contrato-surtido.md).
- [23 pruebas con resultados esperados](../10-pruebas/03-plan-validacion-surtido.md).
- [Migración, conciliación y recuperación](../11-devops/10-migracion-surtido.md).
