# DAT-FAR-006 — Decisiones de Persistencia: Resueltas y Pendientes

**Versión:** 0.2  
**Estado:** parcialmente resuelto por baseline PostgreSQL 18 v0.3.

El baseline físico del **Core Central** ya existe. Este documento conserva las decisiones que todavía requieren ADR, prueba de carga o validación operativa.

## 1. Decisiones resueltas para el Core Central

| Tema | Decisión vigente | Estado |
|---|---|---|
| Motor central | PostgreSQL 18.x | ACEPTADO |
| PK interna | `BIGINT GENERATED ALWAYS AS IDENTITY` | ACEPTADO |
| ID público | `UUID DEFAULT uuidv7()` | ACEPTADO |
| Multi-tenant | `tenant_id` + FKs contextuales | ACEPTADO |
| Inventario | lote + posición + movimiento/kardex + reserva | ACEPTADO |
| Última unidad / concurrencia | `version_lock` + transacción/locking en caso de uso; pruebas de carga pendientes | PARCIAL |
| Integración fiable | Outbox/Inbox + idempotencia | ACEPTADO |
| Documentos/binarios | fuera de PostgreSQL; BD guarda URI/hash/metadatos | ACEPTADO CON PROVEEDOR PENDIENTE |
| Datos de tarjeta | no almacenar PAN/CVV; minimizar alcance PCI | ACEPTADO |

## 2. Decisiones pendientes

1. motor físico del **Store Edge** y capacidades transaccionales en desconexión;
2. política exacta de sobreventa/safety stock durante offline;
3. particionamiento y archivado de ventas, movimientos, auditoría y outbox/inbox;
4. proyecciones CQRS/materialized views para consultas de alto volumen;
5. cifrado por columna/campo para datos que lo requieran;
6. activación de RLS y mecanismo seguro de propagación de `tenant_id`;
7. retención/purga de Outbox, Inbox e intentos de integración;
8. proveedor y política de Object Storage;
9. modalidad de pago/adquirente/tokenización y evaluación final del alcance PCI;
10. topología de alta disponibilidad, RPO/RTO y pruebas de restore;
11. estrategia de particionamiento multi-tenant si el volumen lo justifica;
12. política de índices finales validada con `EXPLAIN (ANALYZE, BUFFERS)` sobre datos representativos.

## 3. Condición para aprobación del DDL

El DDL central puede evolucionar como baseline, pero no se marcará `APROBADO` hasta ejecutar `V001 → V017` en PostgreSQL 18.x real y superar pruebas de constraints, concurrencia, reconstrucción desde cero e idempotencia.
