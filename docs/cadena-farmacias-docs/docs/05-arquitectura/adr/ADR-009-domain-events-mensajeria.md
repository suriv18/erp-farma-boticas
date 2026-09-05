# ADR-009 — Domain Events y mensajería

**Estado:** Aceptado.

## Decisión
Usar Domain Events para desacoplar consecuencias dentro del Core. Cuando un evento deba salir del límite transaccional se persistirá en Outbox. No se obliga Kafka/RabbitMQ desde el inicio; el broker será una decisión posterior según volumen y necesidades.
