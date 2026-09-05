# ADR-007 — ERP Boundary + Anti-Corruption Layer

**Estado:** Aceptado parcial.

## Decisión
Retail, inventario y dispensación no escriben directamente estructuras internas del ERP. Los hechos de negocio generan postings idempotentes a través de un puerto/ACL. Continúa abierta la decisión ERP propio vs. integración con ERP externo.
