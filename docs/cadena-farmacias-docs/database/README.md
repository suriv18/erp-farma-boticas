# Database — PostgreSQL 18

**Baseline consolidado:** v0.3  
**Estado:** borrador técnico avanzado; validación estática realizada, pendiente de ejecución real en PostgreSQL 18.x.

## Fuente de verdad

- `cadena_farmacias_postgresql18.sql`: SQL original y fuente de verdad del modelo.
- `migrations/V001...V017`: distribución modular del SQL original, con los ajustes de ejecución descritos abajo.

Los scripts aportados `V1__init_erp_boticas_postgres_revisado.sql` y `V2__menu_navegacion_rbac.sql` se usaron como **referencia de consolidación**. No deben ejecutarse en paralelo con esta línea base.

## Migraciones

| Migración | Área |
|---|---|
| V001 | schemas, extensiones, tenant |
| V002 | organización, establecimientos, almacenes, terminales y profesionales |
| V003 | catálogo regulatorio, categoría/marca, rubro comercial y SKU retail/farma |
| V004 | proveedores, solicitudes, órdenes y recepción |
| V005 | lotes, posiciones, kardex, reservas, transferencias y conteos |
| V006 | listas de precios y promociones |
| V007 | clientes, caja, ventas, pagos y devoluciones |
| V008 | prescripción y dispensación |
| V009 | productos controlados |
| V010 | fiscal / CPE |
| V011 | recall + farmacovigilancia |
| V012 | ERP financiero + Observatorio |
| V013 | IAM / RBAC / ámbitos / sesiones y FKs de compras/ventas hacia usuarios |
| V014 | auditoría |
| V015 | integraciones + Outbox/Inbox |
| V016 | navegación dinámica RBAC |
| V017 | notificaciones, feature flags y versiones |
| V018 | catálogo SUNAT 06: tipos de documento de identidad y 13 registros iniciales |

## Sincronización con el SQL original

Las 17 migraciones iniciales contienen las 109 tablas del baseline original. Ejecutar en orden
`V001` a `V017` sobre una base vacía; son una línea base, no una actualización
incremental de bases que ya hayan aplicado estas versiones.

Se conservan las definiciones de tablas, índices, función, trigger y datos
iniciales del original, con estos ajustes en las migraciones:

- Las nueve FKs de compras y ventas hacia `sch_seguridad.usuario` se agregan
  mediante `ALTER TABLE` al final de `V013`, cuando la tabla referenciada ya existe.
- En `V002`, se agrega `UNIQUE (tenant_id, id)` a `establecimiento_farmaceutico`
  para respaldar las FKs existentes que referencian ese par de columnas.
- En `V015` y `V017`, tres índices usan la sintaxis correcta
  `CREATE UNIQUE INDEX ... ON tabla (columnas) NULLS NOT DISTINCT WHERE ...`.
- En `V016`, las cinco cláusulas `ON CONFLICT` incluyen `WHERE es_activo = '1'`
  para coincidir con los índices únicos parciales correspondientes.

Estos ajustes no modifican el archivo original. Referencias de sintaxis:
[CREATE INDEX](https://www.postgresql.org/docs/18/sql-createindex.html) e
[INSERT / ON CONFLICT](https://www.postgresql.org/docs/18/sql-insert.html).

Validación realizada: comparación de las 109 definiciones de tablas (considerando
los ajustes anteriores), cobertura de objetos y revisión del orden de creación,
columnas y claves únicas de destino de las 228 FKs. Pendiente: ejecución completa
en PostgreSQL 18.x.

## Catálogo de documentos de identidad SUNAT

V018 agrega `sch_catalogo.tipo_documento_identidad` con los 13 códigos del
catálogo SUNAT 06, verificados en la versión oficial del 26/08/2026.
El modelo actualizado contiene **110 tablas**: baseline V001–V017 (109)
más la nueva tabla V018. El SQL original incluye también esta adición.
En instalaciones nuevas por migraciones, ejecutar V001 a V018; en una base
con V017 aplicada, ejecutar únicamente V018 mediante el gestor de migraciones.

[Fuente, códigos y alcance de integración](catalogos/tipo_documento_identidad_sunat.md).

## Convenciones

- Catálogos globales de referencia: código natural como PK, sin tenant.
- PK interna: `BIGINT GENERATED ALWAYS AS IDENTITY`.
- Identificador público: `UUID DEFAULT uuidv7()`.
- `tenant_id` en datos tenant-scoped.
- FKs contextuales para reducir cruces entre empresa/establecimiento.
- `NULLS NOT DISTINCT` solo cuando `NULL` representa el mismo valor lógico en una clave.
- no guardar PAN/CVV, access tokens, refresh tokens ni secretos en tablas funcionales/logs.
- documentos binarios fuera de PostgreSQL; la BD conserva URI/hash/metadatos.

## Store Edge

Este DDL corresponde al **Core Central**. El motor físico del Store Edge permanece pendiente de ADR; el modelo de sincronización central (`outbox`, `inbox`, `sync_checkpoint`) sí forma parte del Core.
