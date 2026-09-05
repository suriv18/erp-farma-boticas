# ADR-006 — Fiscal Port y ciclo CPE separado de venta

**Estado:** Aceptado.

## Decisión
Venta, pago, generación del CPE, envío y aceptación fiscal son estados/procesos distintos. El dominio Retail se integra mediante un Fiscal Port; PSE/OSE/SEE/otra modalidad aprobada se implementará como adapter.
