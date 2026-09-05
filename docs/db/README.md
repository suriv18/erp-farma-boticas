# Scripts SQL legados

**Estado:** borradores de diseño; no son migraciones ejecutables aprobadas.

Esta carpeta contiene un modelo físico anterior a la línea base documental vigente:

- `V1__init_erp_boticas_postgres_revisado (1).sql`: borrador amplio de 112 tablas en 19 esquemas;
- `V2__menu_navegacion_rbac.sql`: extensión de 2 tablas para navegación dinámica.

Aunque los nombres comienzan con `V1__` y `V2__`, Flyway **no los ejecuta**. La única ubicación de
migraciones runtime es `service-botica/bootstrap-app/src/main/resources/db/migration`, actualmente
sin migraciones SQL de negocio.

Estos scripts se conservan como insumo histórico. Antes de reutilizar una tabla o restricción se debe:

1. vincularla con RF/CU/CA y agregado canónicos;
2. contrastarla con el modelo lógico actual;
3. eliminar capacidades fuera del release;
4. definir ownership, privacidad, retención y concurrencia;
5. dividir el cambio en migraciones pequeñas e irreversibles por versión;
6. validarlo sobre PostgreSQL mediante pruebas de integración.

La fuente de verdad está en
[`docs/cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md`](../cadena-farmacias-docs/docs/00-gobierno/01-fuente-unica-verdad.md).
