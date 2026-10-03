package com.softprimesolutions.inventario.application.usecase.command;

import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.in.RegistrarSalidaVentaUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.MovimientoRegistrado;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.IntStream;
import java.util.stream.Stream;

public final class RegistrarSalidaVentaHandler implements RegistrarSalidaVentaUseCase {

    static final String DOCUMENTO_VENTA = "VENTA";
    static final String MOTIVO_VENTA = "Venta";

    private static final int CLAVE_IDEMPOTENCIA_MAX = 200;
    private static final int ESCALA_MAXIMA = 4;

    private final InventarioWritePort writePort;
    private final RegistrarMovimientoUseCase registrarMovimiento;
    private final ClockPort clock;

    public RegistrarSalidaVentaHandler(
            InventarioWritePort writePort, RegistrarMovimientoUseCase registrarMovimiento, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.registrarMovimiento = Objects.requireNonNull(registrarMovimiento, "registrarMovimiento es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<List<MovimientoResult>, ApplicationError> execute(RegistrarSalidaVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        if (claveInvalida(command.idempotencyKey())) {
            return Result.failure(InventarioErrors.claveIdempotenciaInvalida());
        }
        if (cantidadInvalida(command.cantidad())) return Result.failure(InventarioErrors.cantidadInvalida());
        var previos = movimientosPrevios(command);
        return previos.isEmpty() ? asignar(command) : repetir(command, previos);
    }

    private List<MovimientoResult> movimientosPrevios(RegistrarSalidaVentaCommand command) {
        return IntStream.iterate(1, numero -> numero + 1)
                .mapToObj(numero -> writePort.findMovimientoPorBusinessUuid(
                        command.tenantId(),
                        IdempotencyKeys.businessUuid(IdempotencyKeys.tramo(command.idempotencyKey(), numero))))
                .takeWhile(Optional::isPresent)
                .map(Optional::get)
                .map(MovimientoRegistrado::resultado)
                .toList();
    }

    private static Result<List<MovimientoResult>, ApplicationError> repetir(
            RegistrarSalidaVentaCommand command, List<MovimientoResult> previos) {
        var total = previos.stream().map(MovimientoResult::cantidad).reduce(BigDecimal.ZERO, BigDecimal::add);
        return total.compareTo(command.cantidad()) == 0
                ? Result.success(previos)
                : Result.failure(InventarioErrors.conflictoIdempotencia());
    }

    private Result<List<MovimientoResult>, ApplicationError> asignar(RegistrarSalidaVentaCommand command) {
        var hoy = LocalDate.ofInstant(clock.now(), ZoneOffset.UTC);
        var posiciones = writePort.findPosicionesVendiblesFefo(
                command.tenantId(), command.almacenId(), command.skuId(), hoy);
        var disponible = posiciones.stream().map(PosicionInventario::disponible)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (disponible.compareTo(command.cantidad()) < 0) return Result.failure(InventarioErrors.stockInsuficiente());
        return consumir(command, tramos(posiciones, command.cantidad()));
    }

    private static List<Tramo> tramos(List<PosicionInventario> posiciones, BigDecimal cantidad) {
        var tramos = new ArrayList<Tramo>();
        var pendiente = cantidad;
        for (var posicion : posiciones) {
            if (pendiente.signum() == 0) break;
            var tomar = posicion.disponible().min(pendiente);
            tramos.add(new Tramo(posicion.loteId(), tomar));
            pendiente = pendiente.subtract(tomar);
        }
        return tramos;
    }

    private Result<List<MovimientoResult>, ApplicationError> consumir(
            RegistrarSalidaVentaCommand command, List<Tramo> tramos) {
        Result<List<MovimientoResult>, ApplicationError> acumulado = Result.success(List.of());
        for (var indice = 0; indice < tramos.size(); indice++) {
            var tramo = tramos.get(indice);
            var numero = indice + 1;
            acumulado = acumulado.flatMap(previos -> registrarMovimiento.execute(movimiento(command, tramo, numero))
                    .map(registrado -> Stream.concat(previos.stream(), Stream.of(registrado)).toList()));
        }
        return acumulado;
    }

    private static RegistrarMovimientoCommand movimiento(
            RegistrarSalidaVentaCommand command, Tramo tramo, int numero) {
        var origen = new DocumentoOrigen(
                DOCUMENTO_VENTA, command.ventaId(), command.ventaLineaId(), null);
        return new RegistrarMovimientoCommand(
                command.tenantId(), command.almacenId(), command.skuId(), tramo.loteId(), null, null,
                TipoMovimiento.SALIDA_VENTA.name(), tramo.cantidad(), MOTIVO_VENTA, command.actorId(),
                IdempotencyKeys.tramo(command.idempotencyKey(), numero), origen);
    }

    private static boolean claveInvalida(String clave) {
        return clave == null || clave.isBlank() || clave.length() > CLAVE_IDEMPOTENCIA_MAX;
    }

    private static boolean cantidadInvalida(BigDecimal cantidad) {
        return cantidad == null || cantidad.signum() <= 0 || cantidad.stripTrailingZeros().scale() > ESCALA_MAXIMA;
    }

    private record Tramo(UUID loteId, BigDecimal cantidad) {
    }
}
