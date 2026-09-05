# ADR-003 — CQRS selectivo + Result Pattern

**Estado:** Aceptado.

## Decisión
Separar Commands y Queries en la capa de aplicación. No adoptar Event Sourcing ni bases separadas de lectura/escritura por defecto. Usar Result para errores esperables del negocio y excepciones para fallos técnicos o inesperados.
