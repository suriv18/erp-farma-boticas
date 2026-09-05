# ADR-005 — Outbox/Inbox e idempotencia

**Estado:** Aceptado.

## Decisión
Toda sincronización confiable tienda-central usará identidad estable de mensajes, Outbox en origen, Inbox/deduplicación en destino e idempotency keys para operaciones económicas. No se utilizarán transacciones distribuidas entre tienda y central.
