# API de salida de inventario para ventas (SalidaInventarioApi)

Fecha: 2026-10-03
Contexto: primer paso del slice "turno de caja + venta simple" (RF-POS-001/002/004). `ventas` necesita descontar stock; hoy `inventario::api` solo expone `IngresoInventarioApi` (usada por `compras`).

## Alcance

Dentro: API publica de salida por venta con asignacion FEFO, idempotencia y atomicidad con la transaccion de `ventas`.
Fuera: modulo `ventas` (turno, venta, pagos), reservas de stock, devoluciones, receta/controlados, offline.

## Contrato publico (`inventario::api`)

- `SalidaInventarioApi.registrarSalidaVenta(SalidaVentaSolicitud): Result<SalidaVentaRegistrada, ApplicationError>`
- `SalidaVentaSolicitud(tenantId, almacenId, skuId, cantidad, ventaId, ventaLineaId, actorId, idempotencyKey)`
- `SalidaVentaRegistrada(List<LoteConsumido>)`; `LoteConsumido(movimientoId, loteId, cantidad, stockPosterior)`

## Comportamiento

1. Replay: se detecta por la clave de idempotencia de cada tramo (`clave#n`, derivada de `idempotencyKey`, buscada por su `businessUuid`); si existen, se devuelven los mismos lotes sin mover stock. Quien llama debe enviar una `idempotencyKey` estable por linea de venta (la misma en cada reintento de esa linea y distinta entre lineas).
2. Asignacion FEFO: nuevo `InventarioWritePort.findPosicionesVendiblesFefo(tenantId, almacenId, skuId, hoy)`. Posiciones con lote activo (`es_activo = '1'`), `HABILITADO`, no vencido y `fisica - reservada > 0`, ordenadas por vencimiento asc y numero de lote, bloqueadas con `FOR UPDATE OF p` para serializar ventas concurrentes por posicion. La guarda de cada movimiento usa `Lote.vendible(hoy)` (estado vendible y no vencido).
3. Si la suma disponible < cantidad: `STOCK_INSUFICIENTE`, sin mover nada.
4. Consumo por tramo reutilizando `RegistrarMovimientoUseCase` con `TipoMovimiento.SALIDA_VENTA` (naturaleza `S`, operacion SUNAT `01`, no manual, origen `VENTA`). Clave de idempotencia por tramo: `idempotencyKey#n`.
5. Transaccion: el replay, la lectura FEFO y todos los tramos se ejecutan dentro de `TransaccionPort.ejecutar` (puerto propio de `inventario`, adapter Spring que marca rollback cuando el `Result` es fallo). Sin transaccion externa, un fallo en cualquier tramo revierte los anteriores; con transaccion externa de `ventas` se une a ella, y un fallo de inventario la deja marcada para rollback (la venta completa se revierte).
6. Concurrencia: con las posiciones bloqueadas por `FOR UPDATE`, el reintento interno de `RegistrarMovimientoHandler` por `version_lock` no se activa en este camino (sigue vigente para ajustes e ingresos). Quien llama debe procesar varias salidas de una misma transaccion en orden estable por `skuId` para evitar deadlocks entre SKU.

## Cambios en codigo existente

- `DocumentoOrigen.proveedorId` pasa a ser opcional.
- `TipoMovimiento` agrega `SALIDA_VENTA`. Sin migracion: `movimiento_inventario.tipo_movimiento` no tiene CHECK.
- Bean `SalidaInventarioApi` en `InventarioModuleConfiguration`.

## Pruebas (100% lineas/ramas en archivos nuevos)

- Unitarias: multi-lote, lote vencido/bloqueado/cuarentena excluido, stock insuficiente, replay idempotente, reintento por concurrencia.
- Integracion (PostgreSQL/Testcontainers): salida real FEFO; dos salidas concurrentes sobre el mismo lote no sobrevenden; dos salidas concurrentes, cada una en su propia transaccion externa, confirman ambas sin `UnexpectedRollbackException`; una transaccion externa que falla despues de la salida revierte el descuento.

## Supuestos POR_VALIDAR

- Solo lotes `HABILITADO` son vendibles (CUARENTENA y BLOQUEADO excluidos).
