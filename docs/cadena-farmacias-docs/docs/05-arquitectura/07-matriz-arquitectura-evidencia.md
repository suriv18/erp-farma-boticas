# ARC-FAR-007 — Matriz Decisión Arquitectónica → Evidencia

| Decisión | Tipo | Evidencia / razón | Estado |
|---|---|---|---|
| Core Central Monolito Modular | `TEC/DOM` | 16 Bounded Contexts no justifican 16 deployables; reduce complejidad distribuida | Aceptado |
| DDD + Clean/Ports & Adapters | `TEC/DOM` | protege reglas sanitarias/fiscales de frameworks/proveedores | Aceptado |
| CQRS selectivo | `TEC` | write model con reglas complejas; query model optimizable; Azure CQRS | Aceptado |
| Store Edge opcional | `MKT/TEC` | LS Central y Dynamics soportan POS offline/local DB | Aceptado condicional |
| Base/servicio compartido por tienda | `MKT/TEC` | LS Central documenta Offline POS Server DB | Propuesta preferida |
| Outbox/Inbox | `STD/TEC` | AWS/Azure Transactional Outbox + idempotencia | Aceptado |
| No 2PC tienda-central | `TEC` | resiliencia y operación desconectada | Aceptado |
| Venta separada de CPE | `FISCAL/DOM` | SUNAT mantiene ciclo electrónico y mecanismos de envío/contingencia | Aceptado |
| Fiscal Port/Adapter | `TEC` | evita acoplar dominio a SEE/PSE/OSE | Aceptado |
| ERP Boundary/ACL | `DOM/TEC` | ERP propio/externo aún no decidido | Aceptado parcial |
| `offline_sellable`/cuota de stock | `TEC/DOM` | reduce sobreventa en desconexión | Propuesto |
| Kafka/RabbitMQ | `TEC` | no existe necesidad demostrada | No decidido |
| Event Sourcing | `TEC` | complejidad no justificada actualmente | No adoptado |
| Spring Modulith | `TEC` | herramienta vigente para modularidad Spring | Candidato si Java/Spring |
| arc42 + C4 | `STD/TEC` | documentación y visualización arquitectónica | Aceptado |

## Regla

La evidencia `MKT` demuestra viabilidad/patrón de mercado, no obligación normativa. Las decisiones `TEC` pueden revisarse mediante ADR sin alterar una regla `NORM`.
