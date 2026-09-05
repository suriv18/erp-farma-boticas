# TST-DB-FAR-002 — Validación estática del DDL PostgreSQL 18

**Baseline:** v0.3 consolidado  
**Alcance:** migraciones `V001` a `V017`.

## Resultado actual

| Control | Resultado |
|---|---:|
| Migraciones | 17 |
| Tablas | 108 |
| Foreign Keys | 197 |
| Índices explícitos | 48 |
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
