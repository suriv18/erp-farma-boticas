# DEV-FAR-008 — Persistencia y migraciones

**Motor central:** PostgreSQL 18.

- DDL versionado V001…;
- `IDENTITY` para PK interna y UUIDv7 público;
- FKs/UNIQUE/CHECK en invariantes estructurales;
- transacciones alineadas a agregados;
- optimismo/concurrencia explícitos;
- SQL/JDBC permitido en Query side cuando aporte claridad/rendimiento;
- no navegar libremente entre Bounded Contexts mediante ORM;
- migraciones probadas en PostgreSQL 18 real antes de release;
- Store Edge mantiene DDL independiente hasta cerrar su motor local.

Ver [modelo físico PostgreSQL 18](../06-datos/07-modelo-fisico-postgresql18.md).

## Fuente del modelo y cambios incrementales

El SQL original de database es la referencia del modelo; V001–V017 son su distribución con ajustes de ejecución documentados. No modificar versiones ya aplicadas en una instalación: preparar una migración incremental. El diseño de surtido diverso documenta brechas pendientes y no constituye una migración ejecutada.
