# STD-FAR-002 — Catálogo de Patrones

## 1. Principio

Un patrón se usa para resolver una fuerza concreta, no como requisito de estilo.

| Patrón | Uso candidato | Estado |
|---|---|---|
| Aggregate | límite de consistencia de dominio | Adoptado DDD |
| Value Object | valor con invariantes/semántica | Adoptado DDD |
| Repository | puerto de persistencia de agregados | Adoptado |
| Factory | construcción compleja/validada | Según necesidad |
| Strategy / Policy | precio, lote, CPE, stock, reglas variables | Recomendado |
| Specification | reglas composables reutilizables | Selectivo |
| Adapter | SUNAT, ERP, pagos, storage, DIGEMID | Adoptado |
| Anti-Corruption Layer | proteger modelo frente a ERP/externos | Adoptado |
| State | workflows con comportamiento por estado | Selectivo |
| Chain of Responsibility | pipeline de validaciones | Selectivo |
| Decorator | cross-cutting controlado | Selectivo |
| Domain Event | hechos internos del dominio | Adoptado |
| Transactional Outbox | publicación fiable | Adoptado donde cruza límites |
| Inbox / Idempotent Consumer | deduplicación de sync | Adoptado Store Edge |
| Retry | fallos transitorios | Infraestructura, con límites |
| Circuit Breaker | dependencia remota inestable | Infraestructura, según necesidad |
| Bulkhead | aislamiento de recursos | Según métricas |
| Cache-aside | lecturas de catálogo/precio | Solo si se justifica |
| Saga | procesos distribuidos extensos | No por defecto |
| Event Sourcing | reconstrucción por eventos | No adoptado |

## 2. Patrones que no deben ocultar el dominio

Evitar que `Strategy`, `Specification` o `Factory` degeneren en abstracciones sin lenguaje de negocio.

Correcto:

```text
SeleccionarLoteSalidaPolicy
ValidarRecetaControladaPolicy
ResolverPrecioVentaPolicy
```

Evitar:

```text
GenericStrategy<T>
CommonValidator
BusinessUtils
```

## 3. Retry

Solo para errores transitorios identificables. Un retry no debe repetirse a ciegas sobre una operación no idempotente.

## 4. Saga

Solo evaluar Saga si aparece una transacción distribuida de larga duración con compensaciones reales. El Core modular local no necesita Saga para llamadas dentro de la misma transacción.
