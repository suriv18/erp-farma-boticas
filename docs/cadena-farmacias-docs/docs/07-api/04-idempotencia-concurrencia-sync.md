# API-FAR-004 — Idempotencia, Concurrencia y Sincronización

## 1. Flujos que requieren idempotencia

- confirmar venta;
- registrar pago con proveedor externo;
- emitir/solicitar CPE;
- posting al ERP;
- mensajes Store Edge → Central;
- reintentos de transferencias y devoluciones.

## 2. Header candidato

```http
Idempotency-Key: <opaque-value>
```

El servidor asocia la clave a:

- actor/cliente/establecimiento;
- operación;
- hash semántico del request;
- resultado original;
- período de retención definido.

Reutilizar la misma clave con payload materialmente distinto produce conflicto.

## 3. Concurrencia

Casos críticos:

```text
2 POS
  ↓
mismo SKU/lote
  ↓
última unidad
```

El modelo de persistencia debe asegurar que solo una operación pueda consumir la unidad. La estrategia física (optimistic locking, atomic update, locks, serializable, etc.) se decidirá con pruebas de carga/concurrencia.

## 4. Store Edge

Cada mensaje sincronizado incluirá al menos:

- `messageId`;
- `businessId`;
- `storeId`;
- `terminalId` cuando corresponda;
- `sequenceNo` local si se adopta;
- `schemaVersion`;
- `occurredAt`;
- `correlationId`;
- `causationId`.

Central mantiene Inbox/deduplicación. Store Edge conserva Outbox hasta ACK.
