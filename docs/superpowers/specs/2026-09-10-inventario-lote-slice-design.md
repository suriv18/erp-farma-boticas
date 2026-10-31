# Diseño: primer slice vertical de `inventario` — agregado `Lote`

**Estado:** EN PAUSA — brainstorming interrumpido a pedido del usuario para atender primero el módulo `security`. Este documento registra lo acordado hasta el momento de la pausa. No representa un diseño completo ni aprobado; falta cerrar dominio, infraestructura, API y pruebas antes de pasar a `writing-plans`.

**Contexto:** módulo `inventario` (bounded context `BC-INV`) hoy es un scaffold vacío (`service-botica/modules/inventario/` solo tiene `package-info.java`). Debe construirse siguiendo el patrón arquitectónico Clean Architecture + DDD + Ports & Adapters + CQRS, tomando como referencia el módulo `security` (el más maduro del backend) y el trabajo recién completado en `catalogo`.

## Por qué `Lote` como agregado raíz del primer slice

Investigación de dominio (`docs/cadena-farmacias-docs/docs/04-dominio/*`, `03-requerimientos/*`, `06-datos/*`, ADR-008) concluyó:

- No depende de otros agregados de `BC-INV` — solo referencia `SkuId` (ya existe en `catalogo`).
- Máquina de estados acotada y clara: `HABILITADO → {CUARENTENA | BLOQUEADO | INMOVILIZADO_RECALL | DISPOSICION_FINAL}`.
- Es prerrequisito de `PosicionInventario`, `MovimientoInventario` y `ReservaInventario` (todos referencian `lote_id`) — esos quedan para un slice posterior (ledger + concurrencia optimista, más complejos).
- **Tabla física ya existe**: `sch_farmacia.lote` está completa en `docs/cadena-farmacias-docs/database/migrations/V005__inventario_lotes_transferencias.sql`, que ya corre como migración Flyway real. No se requieren migraciones nuevas de esquema para este slice.
- Cubierto por `CU-INV-002`, `RF-INV-001/002/010/011`, comandos de dominio ya nombrados: `RegistrarLote`, `BloquearLote`, `LiberarLote`.

Trazabilidad de códigos citados en la investigación: `RF-INV-001` a `RF-INV-022`, `RN-INV-001` a `RN-INV-008`, `INV-INV-001` a `INV-INV-007`, `CU-INV-001`/`CU-INV-002`.

## Invariantes/RN críticas para este slice

1. `INV-INV-002` / `RN-INV-002` — lote vencido o no vendible no puede asignarse a venta.
2. `INV-INV-003` — lote en `CUARENTENA`/`BLOQUEADO`/`INMOVILIZADO_RECALL` excluido de disponibilidad hasta liberación autorizada.
3. `INV-INV-006` — todo cambio de estado requiere motivo y actor auditable (ya modelado en el DDL: `motivo_estado`, `bloqueado_at`, `bloqueado_por`).
4. `RN-INV-005` — el lote conserva trazabilidad a su recepción/origen (`origen_recepcion_linea_id`).
5. Invariante de dominio (reflejada en DDL como `ck_lote_fechas`) — `fecha_vencimiento >= fecha_fabricacion`; debe validarse también en el agregado, no solo en BD.

## ADR-008 (consistencia de inventario offline) — implicación para este slice

ADR-008 sigue en estado **Propuesto**, no decidido (pendiente definir con datos reales: safety stock, offline sellable quantity, conflictos, reservas omnicanal, sobreventa). **Este slice asume modo online/autoritativo únicamente** — no resuelve ni anticipa la política offline.

## Decisiones ya acordadas con el usuario

### Alcance de casos de uso
CRUD completo + cambio de estado (no solo registrar/consultar):

- `RegistrarLoteUseCase` (command) — crea el lote con `numeroLote`, `skuId`, `fechaFabricacion`, `fechaVencimiento`, `fabricante`, `origenRecepcionLineaId`.
- `ActualizarLoteUseCase` (command) — corrige datos no críticos (fabricante, fechas) antes de que el lote tenga movimientos asociados.
- `CambiarEstadoLoteUseCase` (command, vía `InventarioControlUseCase` — espejo de `CatalogoControlUseCase` en `catalogo`) — transición de estado con motivo y actor auditable obligatorios.
- `ConsultarLoteUseCase` (query) — detalle por ID.
- `ListarLotesUseCase` (query, paginado) — ver filtros abajo.

### Campo `origenRecepcionLineaId`
**Opcional/nullable** en el command y en la tabla (ya es nullable en el DDL V005). El módulo `compras` (que generaría las líneas de recepción) está vacío todavía — no se bloquea este slice por esa dependencia inexistente. Se completará cuando `compras` exista.

### Permisos RBAC
Mismo patrón que `catalogo`: nueva migración Flyway `V0XX__seed_inventario_permissions.sql` con permisos `inventario.lotes.gestionar` e `inventario.lotes.consultar`, resueltos en cada request desde BD (no confiar en claims del JWT), igual que `security` y `catalogo`.

**Granularidad de las transiciones de estado**: un solo permiso `inventario.lotes.gestionar` cubre todas las transiciones (`CUARENTENA`/`BLOQUEADO`/`INMOVILIZADO_RECALL`/`DISPOSICION_FINAL`/liberación). Mismo criterio que `CatalogoControlUseCase` en `catalogo` — no hay todavía un requisito de negocio que exija separar, por ejemplo, quién bloquea de quién libera (eso podría revisarse cuando exista un flujo real de recall con roles QA distintos).

### Filtros de `ListarLotes` (paginado)
Confirmados tras validar contra prácticas estándar de WMS farmacéutico (búsqueda web):

- `skuId` — lotes de un producto/SKU específico.
- `estado` — `HABILITADO`/`CUARENTENA`/`BLOQUEADO`/`INMOVILIZADO_RECALL`/`DISPOSICION_FINAL`.
- `fechaVencimientoDesde` / `fechaVencimientoHasta` — rango de vencimiento.
- `diasParaVencer` — alternativa de conveniencia al rango (ej. `diasParaVencer=30`), común en reportes FEFO/alertas de vencimiento próximo.
- `fabricante` — filtro por fabricante/proveedor de origen (útil para recalls dirigidos a un fabricante específico).
- `q` — búsqueda libre por número de lote (mismo patrón `q` usado en `catalogo`).
- `page` / `size` — paginado estándar (respuesta tipo `PaginaResponse`, igual que `catalogo`/`security`).

### Persistencia (CQRS pragmático)
Mismo patrón que `catalogo`: escritura JPA en `persistence/write` (`LoteJpaEntity` + `LoteJpaRepository` + `LoteJpaWriteAdapter`), lectura JDBC en `persistence/read` (`LoteJdbcReadAdapter` + proyección + repository JDBC) para consultas y listado paginado. Sin bases de datos separadas — regla del `CLAUDE.md` raíz del repo.

## Pendiente antes de reanudar el brainstorming

- Modelo de dominio detallado: atributos exactos del agregado `Lote`, value objects (`NumeroLote`, `EstadoLote`, etc.), invariantes codificadas en el constructor/métodos de transición.
- Puertos de entrada/salida (`application/port/in`, `application/port/out`).
- Estructura de infraestructura (entidades JPA, proyecciones JDBC, mapeos).
- Diseño de la API REST (`LoteController`, DTOs request/response, mapper) — aplicar también la lección de la revisión de `catalogo`: evaluar si sub-recursos (si los hubiera) deben ir en un controller separado desde el inicio.
- Estrategia de pruebas (unitarias de dominio, handlers, y al menos un test de integración API por patrón `IamApiIntegrationTest`).
- Aprobación final del usuario y escritura del spec completo antes de invocar `writing-plans`.

## Próximo paso

Retomar este brainstorming después de resolver los pendientes del módulo `security` que el usuario indicó atender primero.
