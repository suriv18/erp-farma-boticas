# Fase 7A — Datos

**Estado:** modelo conceptual/lógico + baseline físico PostgreSQL 18 v0.3; pendiente de ejecución real y validación de Store Edge.

Esta fase traduce el dominio DDD a estructuras de información sin convertir automáticamente cada entidad o Value Object en una tabla.

## Documentos

- [DAT-FAR-001 — Modelo conceptual](01-modelo-conceptual.md)
- [DAT-FAR-002 — Modelo lógico por agregado](02-modelo-logico-por-agregado.md)
- [DAT-FAR-003 — Propiedad de datos Central vs Store Edge](03-propiedad-datos-central-store-edge.md)
- [DAT-FAR-004 — Clasificación, privacidad y retención](04-clasificacion-retencion-datos.md)
- [DAT-FAR-005 — Matriz dato → fuente](05-matriz-dato-fuente.md)
- [DAT-FAR-006 — Decisiones de persistencia pendientes](06-decisiones-persistencia-pendientes.md)

## Regla

Los modelos conceptuales/lógicos continúan siendo la referencia de dominio. PostgreSQL 18 ya fue seleccionado para el Core Central; RLS y el motor local del Store Edge siguen pendientes de decisión/validación.

- [DAT-FAR-007 — Modelo físico PostgreSQL 18](07-modelo-fisico-postgresql18.md)
- [DAT-FAR-008 — Consolidación V1/V2 y enriquecimiento de campos](08-consolidacion-v1-v2-campos.md)

- [DAT-FAR-009 — Adecuación al surtido diverso en Perú](09-adecuacion-surtido-multiproducto-peru.md)

Revisión documental: 2026-09-15. El SQL original gobierna el modelo físico; las migraciones incorporan los ajustes de ejecución descritos en database/README. Surtido diverso: soporte parcial, con brechas de inventario y admisibilidad.

- [DAT-FAR-010 — Diseño detallado del surtido](10-diseno-detallado-surtido.md)

El diseño del nuevo surtido está definido; su soporte físico permanece pendiente de implementación.
