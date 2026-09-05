# ARC-FAR-005 — Sincronización Tienda ↔ Central

## 1. Principio

No se utilizarán transacciones distribuidas 2PC entre tienda y central.

La sincronización será asincrónica, explícita, trazable e idempotente.

## 2. Patrón

```text
Transacción de tienda
        ↓
Store DB + Outbox
(misma transacción local)
        ↓
Sync Worker
        ↓
Central Inbox
        ↓
validación idempotencia
        ↓
procesamiento
        ↓
ACK / rechazo recuperable
```

Transactional Outbox evita el dual-write `guardar venta + enviar mensaje` en dos operaciones independientes. AWS/Azure recomiendan consumidores idempotentes porque puede existir entrega duplicada.

## 3. Envelope mínimo

Todo mensaje tienda-central debe tener como mínimo:

```text
message_id
message_type
schema_version
store_id
terminal_id (si aplica)
business_id
sequence_no (cuando orden importe)
occurred_at
produced_at
correlation_id
causation_id
idempotency_key
payload
```

## 4. Inbox

Central conserva registro de mensajes procesados para:

- deduplicar;
- reintentar;
- reconstruir incidentes;
- detectar gaps de secuencia cuando aplique.

## 5. Datos HQ → Tienda

Se distribuyen como versiones/publicaciones, no como mutaciones arbitrarias.

Ejemplos:

- catálogo/SKU;
- condición de venta;
- precio;
- promoción;
- configuración;
- recall/bloqueos;
- operadores/políticas mínimas.

## 6. Política de ownership/conflictos

| Dato | Owner lógico | Estrategia |
|---|---|---|
| Maestro producto regulado | Central | Central publica; tienda consume |
| Precio/promoción | Central | versión efectiva; venta conserva snapshot |
| Venta local | Tienda origen | append/idempotent ingest |
| Caja/turno | Tienda | central consolida |
| Movimiento POS de lote | Tienda origen | central ingiere y reconcilia |
| Recall/bloqueo | Central/regulatorio | prioridad alta hacia tienda |
| CPE | Fiscal context | estado explícito, no duplicar |

No se utiliza `last-write-wins` como regla genérica.

## 7. Estados de sincronización

Candidato:

```text
PENDING
SENDING
ACKNOWLEDGED
RETRYABLE_ERROR
DEAD_LETTER
REJECTED_BUSINESS
```

Estos estados son `TEC`, no sanitarios.

## 8. Reintentos

- backoff;
- límite configurable;
- errores transitorios vs permanentes;
- DLQ/cola de intervención;
- nunca duplicar venta, CPE o posting por un retry.

## 9. Observabilidad

Métricas mínimas:

- última sincronización exitosa por tienda;
- edad de la cola;
- mensajes pendientes;
- retries;
- rechazos;
- lag de maestros;
- gaps de secuencia;
- versión local vs central.

Los umbrales/SLO quedan `POR_VALIDAR`.
