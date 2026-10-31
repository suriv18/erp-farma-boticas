package com.softprimesolutions.ventas.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.util.Objects;
import java.util.UUID;

public final class CerrarTurnoHandler implements CerrarTurnoUseCase {

    private final TurnoWritePort turnos;
    private final ConsultarTurnosUseCase consultas;
    private final TransaccionPort transaccion;
    private final ClockPort clock;

    public CerrarTurnoHandler(
            TurnoWritePort turnos, ConsultarTurnosUseCase consultas, TransaccionPort transaccion, ClockPort clock) {
        this.turnos = Objects.requireNonNull(turnos, "turnos es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "consultas es obligatorio");
        this.transaccion = Objects.requireNonNull(transaccion, "transaccion es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> execute(CerrarTurnoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return transaccion.ejecutar(() -> cerrar(command))
                .flatMap(turnoId -> consultas.obtener(new ObtenerTurnoQuery(command.tenantId(), turnoId)));
    }

    private Result<UUID, ApplicationError> cerrar(CerrarTurnoCommand command) {
        var turno = turnos.findPorIdParaActualizar(command.tenantId(), command.turnoId());
        if (turno.isEmpty()) return Result.failure(VentasErrors.turnoNoEncontrado());
        var totalVentas = turnos.totalVentasEfectivo(command.tenantId(), command.turnoId());
        return turno.get().cerrar(totalVentas, command.totalDeclarado(), command.observacion(), clock.now())
                .fold(
                        cerrado -> guardar(cerrado, new Actor(command.actorId())),
                        error -> Result.<UUID, ApplicationError>failure(VentasErrors.fromDomain(error)));
    }

    private Result<UUID, ApplicationError> guardar(TurnoCaja cerrado, Actor actor) {
        if (!turnos.actualizarCierre(cerrado, actor)) return Result.failure(VentasErrors.modificacionConcurrente());
        return Result.success(cerrado.id());
    }
}
