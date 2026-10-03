package com.softprimesolutions.inventario.application.usecase.command;

import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.in.ReintegrarSalidasDeVentaUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.SalidaDeVenta;
import com.softprimesolutions.inventario.application.port.out.TransaccionPort;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;

public final class ReintegrarSalidasDeVentaHandler implements ReintegrarSalidasDeVentaUseCase {

    static final String DOCUMENTO_ANULACION = "ANULACION_VENTA";
    static final String MOTIVO_ANULACION = "Anulacion de venta";

    private static final Comparator<SalidaDeVenta> ORDEN_DE_BLOQUEO = Comparator
            .comparing(SalidaDeVenta::skuId)
            .thenComparing(SalidaDeVenta::fechaVencimiento)
            .thenComparing(SalidaDeVenta::numeroLote);

    private final InventarioWritePort writePort;
    private final RegistrarMovimientoUseCase registrarMovimiento;
    private final TransaccionPort transaccion;

    public ReintegrarSalidasDeVentaHandler(
            InventarioWritePort writePort, RegistrarMovimientoUseCase registrarMovimiento,
            TransaccionPort transaccion) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.registrarMovimiento = Objects.requireNonNull(registrarMovimiento, "registrarMovimiento es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
    }

    @Override
    public Result<List<MovimientoResult>, ApplicationError> execute(ReintegrarSalidasDeVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return transaccion.ejecutar(() -> reintegrar(command));
    }

    private Result<List<MovimientoResult>, ApplicationError> reintegrar(ReintegrarSalidasDeVentaCommand command) {
        var salidas = writePort.findSalidasDeVenta(command.tenantId(), command.ventaId()).stream()
                .sorted(ORDEN_DE_BLOQUEO)
                .toList();
        if (salidas.isEmpty()) return Result.failure(InventarioErrors.salidasNoEncontradas());
        Result<List<MovimientoResult>, ApplicationError> acumulado = Result.success(List.of());
        for (var salida : salidas) {
            acumulado = acumulado.flatMap(previos -> registrarMovimiento.execute(movimiento(command, salida))
                    .map(registrado -> Stream.concat(previos.stream(), Stream.of(registrado)).toList()));
        }
        return acumulado;
    }

    private static RegistrarMovimientoCommand movimiento(
            ReintegrarSalidasDeVentaCommand command, SalidaDeVenta salida) {
        var origen = new DocumentoOrigen(DOCUMENTO_ANULACION, command.ventaId(), salida.movimientoId(), null);
        return new RegistrarMovimientoCommand(
                command.tenantId(), salida.almacenId(), salida.skuId(), salida.loteId(), null, null,
                TipoMovimiento.ANULACION_VENTA.name(), salida.cantidad(), MOTIVO_ANULACION, command.actorId(),
                IdempotencyKeys.reverso(salida.movimientoId()), origen);
    }
}
