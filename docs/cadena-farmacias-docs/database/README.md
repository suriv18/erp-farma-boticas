# Database — PostgreSQL 18

**Baseline consolidado:** v0.3  
**Estado:** borrador técnico avanzado; validación estática realizada, pendiente de ejecución real en PostgreSQL 18.x.

## Fuente de verdad

- `migrations/V001...V017`: migraciones modulares.
- `cadena_farmacias_postgresql18.sql`: consolidado generado a partir de las migraciones.

Los scripts aportados `V1__init_erp_boticas_postgres_revisado.sql` y `V2__menu_navegacion_rbac.sql` se usaron como **referencia de consolidación**. No deben ejecutarse en paralelo con esta línea base.

## Migraciones

| Migración | Área |
|---|---|
| V001 | schemas, extensiones, tenant |
| V002 | organización, establecimientos, almacenes, terminales y profesionales |
| V003 | catálogo regulatorio, categoría/marca y SKU |
| V004 | proveedores, solicitudes, órdenes y recepción |
| V005 | lotes, posiciones, kardex, reservas, transferencias y conteos |
| V006 | listas de precios y promociones |
| V007 | clientes, caja, ventas, pagos y devoluciones |
| V008 | prescripción y dispensación |
| V009 | productos controlados |
| V010 | fiscal / CPE |
| V011 | recall + farmacovigilancia |
| V012 | ERP financiero + Observatorio |
| V013 | IAM / RBAC / ámbitos / sesiones |
| V014 | auditoría |
| V015 | integraciones + Outbox/Inbox |
| V016 | navegación dinámica RBAC |
| V017 | notificaciones, feature flags y versiones |

## Convenciones

- PK interna: `BIGINT GENERATED ALWAYS AS IDENTITY`.
- Identificador público: `UUID DEFAULT uuidv7()`.
- `tenant_id` en datos tenant-scoped.
- FKs contextuales para reducir cruces entre empresa/establecimiento.
- `NULLS NOT DISTINCT` solo cuando `NULL` representa el mismo valor lógico en una clave.
- no guardar PAN/CVV, access tokens, refresh tokens ni secretos en tablas funcionales/logs.
- documentos binarios fuera de PostgreSQL; la BD conserva URI/hash/metadatos.

## Store Edge

Este DDL corresponde al **Core Central**. El motor físico del Store Edge permanece pendiente de ADR; el modelo de sincronización central (`outbox`, `inbox`, `sync_checkpoint`) sí forma parte del Core.
