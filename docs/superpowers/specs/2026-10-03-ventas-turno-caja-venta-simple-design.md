# Modulo ventas: turno de caja y venta simple

Fecha: 2026-10-03
Contexto: slice core del POS (RF-POS-001, RF-POS-002, RF-POS-004, RF-POS-007 solo efectivo). Depende de `SalidaInventarioApi` (ver `2026-10-03-salida-venta-inventario-design.md`). Esquema fisico existente: `sch_venta` en V007.

## Alcance

Dentro: abrir/consultar/cerrar turno de caja; registrar venta presencial con lineas y un pago en efectivo; consultar ventas.
Fuera: CPE/fiscal, receta y controlados, promociones y precios de lista, clientes, otros medios de pago, anulacion/devolucion (requiere API de reintegro en inventario), movimientos de caja manuales, offline.

## API REST (`/api/v1/ventas`)

- `POST /turnos` (terminalId, fondoInicial) abre turno.
- `GET /turnos/actual?terminalId=` devuelve el turno abierto de la terminal.
- `POST /turnos/{id}/cierre` (totalDeclarado, observacion).
- `GET /turnos/{id}`.
- `POST /ventas` con cabecera `Idempotency-Key`: terminalId, almacenId, lineas[skuId, cantidad, precioUnitario], pago[montoRecibido] (efectivo). El turno se deduce del turno abierto de la terminal.
- `GET /ventas/{id}`; `GET /ventas` paginado por establecimiento y rango de fechas.

## Reglas

Turno:
- Un solo turno abierto por terminal (`uk_turno_terminal_abierto`); un segundo intento es conflicto.
- Terminal `ACTIVO` y del tenant del JWT.
- Cierre `ABIERTO` -> `CERRADO` directo (sin `EN_ARQUEO`); `total_ventas_sistema` = suma de ventas en efectivo, `total_sistema` = fondo + ventas, `diferencia` = declarado - sistema. No se cierra un turno cerrado.

Venta:
- Requiere turno abierto en la terminal.
- Almacen del mismo establecimiento de la terminal y con `permite_venta`.
- SKU operable; cantidad > 0 con hasta 4 decimales; precio unitario >= 0; maximo 100 lineas.
- `total_linea` = round(cantidad x precio, 2); `subtotal` = suma de lineas; `impuesto` = 0; `total` = `subtotal`; total > 0. IGV POR_VALIDAR hasta el slice CPE.
- Efectivo: `montoRecibido` >= total; `vuelto` = recibido - total.
- `numero_operacion` = `{codigo terminal}-{secuencia 6 digitos}`; contador por terminal en `sch_venta.secuencia_operacion`, incrementado en la misma transaccion (POR_VALIDAR).

## Flujo de registro de venta

1. Idempotencia por `Idempotency-Key` (`uk_venta_idempotency`): misma clave y huella devuelve la misma venta; huella distinta es conflicto.
2. Validar turno, terminal, almacen, lineas y totales.
3. Transaccion unica: insertar `venta` y `venta_linea`; por cada linea llamar a `SalidaInventarioApi.registrarSalidaVenta` (ventaId, ventaLineaId, clave `{Idempotency-Key}:{n linea}`) y guardar cada lote en `venta_linea_lote`; insertar `pago_venta` y avanzar el correlativo.
4. `INV_STOCK_INSUFICIENTE` -> 409 y rollback completo.
5. `INV_MODIFICACION_CONCURRENTE` -> reintentar la venta completa en transaccion nueva (maximo 3) mediante `TransaccionPort`, como `compras`.

## Capas y dependencias

- Patron de `compras`: domain / application (ports in-out, handlers CQRS con `Result`) / infrastructure (JDBC) / api.
- Modulith `allowedDependencies = {"inventario::api", "organizacion::api"}` (se retiran `clientes::api` y `pagos::api`); `build.gradle` suma `:modules:inventario`.
- Persistencia JDBC sobre `sch_venta`; referencias de organizacion/catalogo por SQL.
- Actor: del JWT (`sub`) a `usuario_id` por SQL; sin `ActorId` compartido.
- Dominio: `TurnoCaja`, `Venta` con lineas y pago, dinero en `BigDecimal` escala 2.

## Migraciones (desde V033)

- V033: `created_by`/`updated_by` a `VARCHAR(36)` en las tablas de `sch_venta` tocadas; `huella_solicitud` en `venta`; tabla `secuencia_operacion`; medio de pago `EFECTIVO` por tenant.
- V034: permisos `ventas.turnos.abrir|cerrar|consultar`, `ventas.ventas.registrar|consultar`, concedidos al rol `ADMIN` de FARMALAB (patron V032; modulo `VENTAS` ya existe en V016).

## Pruebas

- Unitarias con 100% de lineas y ramas en archivos nuevos (dominio, handlers, adapters con stub JDBC, controllers).
- `VentasApiIntegrationTest` (login real, PostgreSQL): abrir turno, vender con FEFO, 409 por stock insuficiente, permisos.
- `VentasConcurrencyIntegrationTest`: dos ventas simultaneas sobre el mismo lote sin sobreventa.

## Supuestos POR_VALIDAR

IGV en 0 hasta CPE; correlativo por terminal para `numero_operacion`; cierre de turno sin arqueo intermedio.
