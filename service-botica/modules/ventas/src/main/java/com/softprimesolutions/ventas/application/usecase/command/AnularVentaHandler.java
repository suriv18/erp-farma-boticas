package com.softprimesolutions.ventas.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AnularVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort.VentaParaAnular;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.domain.model.AnulacionVenta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.util.Objects;
import java.util.UUID;

public final class AnularVentaHandler implements AnularVentaUseCase {

    private static final int MAX_INTENTOS = 3;

    private final AnulacionWritePort ventas;
    private final ReintegroInventarioPort inventario;
    private final ConsultarVentasUseCase consultas;
    private final TransaccionPort transaccion;
    private final ClockPort clock;

    public AnularVentaHandler(
            AnulacionWritePort ventas, ReintegroInventarioPort inventario, ConsultarVentasUseCase consultas,
            TransaccionPort transaccion, ClockPort clock) {
        this.ventas = Objects.requireNonNull(ventas, "ventas es obligatorio");
        this.inventario = Objects.requireNonNull(inventario, "inventario es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "consultas es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> execute(AnularVentaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var resultado = intentar(command);
        for (var intento = 1; intento < MAX_INTENTOS && esConcurrencia(resultado); intento++) {
            resultado = intentar(command);
        }
        return resultado;
    }

    private Result<VentaResult, ApplicationError> intentar(AnularVentaCommand command) {
        return transaccion.ejecutar(() -> anular(command))
                .flatMap(ventaId -> consultas.obtener(new ObtenerVentaQuery(command.tenantId(), ventaId)));
    }

    private Result<UUID, ApplicationError> anular(AnularVentaCommand command) {
        var venta = ventas.bloquearVenta(command.tenantId(), command.ventaId());
        if (venta.isEmpty()) return Result.failure(VentasErrors.ventaNoEncontrada());
        return VentasErrors.fromDomain(AnulacionVenta.validar(
                        venta.get().estado(), venta.get().estadoTurno(), command.motivo()))
                .flatMap(motivo -> reintegrarYMarcar(command, venta.get(), motivo));
    }

    private Result<UUID, ApplicationError> reintegrarYMarcar(
            AnularVentaCommand command, VentaParaAnular venta, String motivo) {
        return inventario.reintegrar(command.tenantId(), venta.id(), command.actorId())
                .flatMap(lotes -> marcar(command, venta, motivo));
    }

    private Result<UUID, ApplicationError> marcar(AnularVentaCommand command, VentaParaAnular venta, String motivo) {
        var marcada = ventas.marcarAnulada(
                command.tenantId(), venta.id(), new Actor(command.actorId()), motivo, clock.now());
        if (!marcada) return Result.failure(VentasErrors.modificacionConcurrente());
        return Result.success(venta.id());
    }

    private static boolean esConcurrencia(Result<VentaResult, ApplicationError> resultado) {
        return resultado.fold(venta -> false, error -> VentasErrors.CONCURRENCIA.equals(error.code()));
    }
}
