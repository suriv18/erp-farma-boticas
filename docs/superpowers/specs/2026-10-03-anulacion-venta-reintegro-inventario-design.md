# Anulacion de venta y API de reintegro de inventario

Fecha: 2026-10-03
Contexto: RF-POS-014 (cancelar venta antes de cierre). Depende de `ventas` (turno y venta en efectivo) y de `SalidaInventarioApi`. La devolucion comercial (RF-DEV) queda para un segundo spec que reutiliza el reintegro.

## Alcance

Dentro: anular una venta CONFIRMADA mientras su turno sigue abierto; API publica de reintegro en `inventario`; migracion V035.
Fuera: devolucion parcial/posterior al turno, aprobacion, reembolso y disposicion sanitaria; nota de credito y CPE (anular un comprobante emitido exigira nota de credito cuando exista CPE).

## API de inventario (`inventario::api`)

- `AnulacionInventarioApi.reintegrarSalidasDeVenta(ReintegroVentaSolicitud): Result<ReintegroVentaRegistrado, ApplicationError>`.
- `ReintegroVentaSolicitud(tenantId, ventaId, actorId)`; `ReintegroVentaRegistrado(List<MovimientoReintegrado>)`; `MovimientoReintegrado(movimientoId, loteId, cantidad, stockPosterior)`. Constantes de codigo: concurrencia y `INV_SALIDAS_NO_ENCONTRADAS`.
- Inventario es dueno del kardex: localiza los movimientos `SALIDA_VENTA` con `documento_tipo='VENTA'` y `documento_uuid=ventaId` y aplica un ingreso inverso por cada uno sobre el mismo almacen, SKU y lote. `ventas` no envia lotes ni cantidades, de modo que no se puede reintegrar mas de lo vendido.
- Idempotencia: la clave de cada reverso se deriva del id del movimiento original; reintentar devuelve los mismos reversos sin mover stock. Sin salidas encontradas: `INV_SALIDAS_NO_ENCONTRADAS`.
- Movimiento nuevo `TipoMovimiento.ANULACION_VENTA`: naturaleza `E`, no manual, tipo de operacion SUNAT `05` (POR_VALIDAR), origen `ANULACION_VENTA` con la venta como documento. Reutiliza `RegistrarMovimientoUseCase`.
- Estado del lote: el reintegro se acepta aunque el lote ya no admita ingresos (recall, vencido); el estado del lote sigue impidiendo venderlo (POR_VALIDAR).
- Orden de bloqueo: reversos aplicados por SKU y luego FEFO (fecha de vencimiento, numero de lote), el mismo orden que las ventas.
- Transaccion: propia con `TransaccionPort` de inventario (REQUIRED); se une a la de `ventas`.

## ventas

- `POST /api/v1/ventas/ventas/{ventaId}/anulacion` con `{"motivo"}`; permiso critico `ventas.ventas.anular`; motivo 1..500 caracteres; auditoria de quien y cuando anulo.
- Reglas: solo venta `CONFIRMADA` con turno `ABIERTO` (409 `VEN_TURNO_NO_ABIERTO` si el turno esta cerrado); una venta ya anulada es 409 `VEN_VENTA_ESTADO_INVALIDO`; venta inexistente 404.
- Transaccion unica: bloquea la venta `FOR UPDATE` y el turno `FOR SHARE` (un cierre concurrente espera); venta -> `ANULADA`; pago -> `REVERSADO`; reintegro de stock via `AnulacionInventarioApi`. El turno deja de contar la venta sin cambiar su consulta de totales (ya suma solo ventas CONFIRMADA con pago CONFIRMADO).
- Concurrencia: ante `INV_MODIFICACION_CONCURRENTE` se reintenta la anulacion completa (maximo 3), como la venta.
- Respuesta: `VentaResponse` con estado `ANULADA` y bloque `anulacion` (anuladaAt, anuladaPorId, motivo), null mientras no este anulada.

## Migracion V035

- `sch_venta.venta`: `anulada_at TIMESTAMPTZ`, `anulada_por_usuario_id BIGINT`, `motivo_anulacion VARCHAR(500)`.
- Permiso `ventas.ventas.anular` (critico) sembrado y concedido al rol `ADMIN` de FARMALAB (patron V034).

## Pruebas

- Unitarias con 100% de lineas y ramas en archivos nuevos.
- Integracion HTTP: anulacion con reintegro a los lotes originales (incluida venta multi-lote), kardex con `ANULACION_VENTA`, pago `REVERSADO`, turno que no cuenta la venta anulada, doble anulacion 409, turno cerrado 409, permisos.
- Concurrencia con datos confirmados: anulacion contra cierre de turno, doble anulacion simultanea, fallo de inventario que deja la venta `CONFIRMADA` intacta, anulacion contra venta sobre los mismos lotes sin deadlock.

## Supuestos POR_VALIDAR

Tipo de operacion SUNAT `05`; reintegro permitido sobre lotes bloqueados/vencidos; anulacion solo dentro del turno abierto; control de autorizacion de un solo paso (permiso critico + motivo), con aprobacion de supervisor como evolucion (RF-POS-022).
