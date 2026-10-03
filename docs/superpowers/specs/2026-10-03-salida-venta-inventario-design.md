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

1. Replay: si ya existen movimientos con origen `VENTA` + `ventaLineaId`, se devuelven los mismos lotes sin mover stock.
2. Asignacion FEFO: nuevo `InventarioWritePort.findPosicionesVendiblesFefo(tenantId, almacenId, skuId, hoy)`. Posiciones con lote `HABILITADO` (`EstadoLote.vendible()`), no vencido y `fisica - reservada > 0`, ordenadas por vencimiento asc y numero de lote.
3. Si la suma disponible < cantidad: `STOCK_INSUFICIENTE`, sin mover nada.
4. Consumo por tramo reutilizando `RegistrarMovimientoUseCase` con `TipoMovimiento.SALIDA_VENTA` (naturaleza `S`, operacion SUNAT `01`, no manual, origen `VENTA`). Clave de idempotencia por tramo: `idempotencyKey#n`.
5. Transaccion: `TransactionOperations` ya existente se une a la transaccion externa de `ventas`; un fallo revierte venta y descuentos.

## Cambios en codigo existente

- `DocumentoOrigen.proveedorId` pasa a ser opcional.
- `TipoMovimiento` agrega `SALIDA_VENTA`. Sin migracion: `movimiento_inventario.tipo_movimiento` no tiene CHECK.
- Bean `SalidaInventarioApi` en `InventarioModuleConfiguration`.

## Pruebas (100% lineas/ramas en archivos nuevos)

- Unitarias: multi-lote, lote vencido/bloqueado/cuarentena excluido, stock insuficiente, replay idempotente, reintento por concurrencia.
- Integracion (PostgreSQL/Testcontainers): salida real FEFO; dos salidas concurrentes sobre el mismo lote no sobrevenden.

## Supuestos POR_VALIDAR

- Solo lotes `HABILITADO` son vendibles (CUARENTENA y BLOQUEADO excluidos).
