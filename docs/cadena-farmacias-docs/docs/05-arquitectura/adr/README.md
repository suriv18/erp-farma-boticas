# Architecture Decision Records — Cadena de Farmacias

| ADR | Decisión | Estado |
|---|---|---|
| [ADR-001](ADR-001-arquitectura-global.md) | Core Central Modular + Store Edge opcional | Aceptado |
| [ADR-002](ADR-002-ddd-clean-ports-adapters.md) | DDD + Clean Architecture + Ports & Adapters | Aceptado |
| [ADR-003](ADR-003-cqrs-result.md) | CQRS selectivo + Result Pattern | Aceptado |
| [ADR-004](ADR-004-pos-online-offline-capable.md) | POS online-first con perfil offline-capable | Aceptado condicional |
| [ADR-005](ADR-005-sincronizacion-outbox-idempotencia.md) | Outbox/Inbox + idempotencia tienda-central | Aceptado |
| [ADR-006](ADR-006-integracion-fiscal-cpe.md) | Fiscal Port/Adapter y ciclo CPE separado de venta | Aceptado |
| [ADR-007](ADR-007-limite-erp.md) | ERP Boundary + ACL; ERP propio/externo aún abierto | Aceptado parcial |
| [ADR-008](ADR-008-consistencia-inventario-offline.md) | Política de consistencia de stock offline | Propuesto |
| [ADR-009](ADR-009-domain-events-mensajeria.md) | Domain Events + Outbox; broker no obligatorio | Aceptado |
| [ADR-010](ADR-010-navegacion-dinamica-rbac.md) | Navegación dinámica desacoplada de RBAC | Aceptado |

## Regla

Un ADR aceptado documenta una decisión técnica. No se debe presentar como requisito normativo sanitario/fiscal salvo que su contexto cite la fuente correspondiente.
