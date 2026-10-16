# TST-DB-FAR-002 — Validación estática del DDL PostgreSQL 18

**Baseline:** SQL original y V001–V017 sincronizados; revisión 2026-09-15  
**Alcance:** migraciones `V001` a `V017`.

## Resultado actual

| Control | Resultado |
|---|---:|
| Migraciones | 17 |
| Esquemas | 15 |
| Tablas | 109 |
| Foreign Keys | 228 |
| Índices explícitos | 137 |
| FK a tabla inexistente | 0 |
| FK a columna inexistente | 0 |
| FK a destino sin PK/UNIQUE compatible | 0 |
| Nombres de índices explícitos duplicados | 0 |

Las tablas de catálogo regulatorio global (`condicion_venta`, `forma_farmaceutica`, `via_administracion`, `unidad_medida`, `clasificacion_controlada`, `principio_activo`, `producto_regulado`) no llevan `tenant_id` deliberadamente; los SKU/configuraciones comerciales sí son tenant-scoped.

## Validaciones adicionales realizadas

- claves compuestas multi-tenant en organización/stock/venta/seguridad;
- `NULLS NOT DISTINCT` en claves donde el `NULL` representa el mismo scope lógico;
- una línea de venta puede usar varios lotes;
- devolución no repone stock automáticamente;
- `cliente_consentimiento` separado del cliente/venta;
- navegación visual separada de RBAC efectivo;
- integración evita persistir request/response crudos como mecanismo de logging por defecto.

## Pendiente antes de aprobación

1. Ejecutar `V001 → V017` sobre PostgreSQL 18.x real.
2. Ejecutar pruebas de constraints y concurrencia.
3. Probar migración limpia y reconstrucción desde cero.
4. Ejecutar pruebas de idempotencia de venta/CPE/posting/Inbox.
5. Validar permisos/menú con escenarios tenant/empresa/establecimiento.
6. Revisar índices con `EXPLAIN (ANALYZE, BUFFERS)` usando volumen representativo.

El estado continúa como **borrador técnico avanzado**, no `APROBADO`, hasta completar la ejecución real.

## Alcance de la evidencia y brechas

La revisión estática comprueba existencia, orden, columnas y PK/UNIQUE destino de las FKs; no ejecuta PostgreSQL ni demuestra todas las invariantes del negocio. Se compararon las definiciones con las excepciones documentadas en [database/README](../../database/README.md).

Adicionalmente se requieren pruebas de inventario sin lote, lote sin vencimiento y cruce SKU/lote, además de admisibilidad por establecimiento. El DDL actual no soporta todavía los dos primeros flujos. Especificaciones en [REQ-FAR-MULTI](../03-requerimientos/09-surtido-multiproducto.md).


Plan de ejecución para la evolución documentada: [TST-MULTI](03-plan-validacion-surtido.md), con 23 escenarios. Su estado es definido, no ejecutado.
