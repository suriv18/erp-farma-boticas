package com.softprimesolutions.ventas.application.usecase.command;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.util.Objects;

public final class AbrirTurnoHandler implements AbrirTurnoUseCase {

    private final TurnoWritePort turnos;
    private final ReferenciasVentasPort referencias;
    private final ConsultarTurnosUseCase consultas;
    private final IdentifierGenerator identifiers;
    private final ClockPort clock;

    public AbrirTurnoHandler(
            TurnoWritePort turnos, ReferenciasVentasPort referencias, ConsultarTurnosUseCase consultas,
            IdentifierGenerator identifiers, ClockPort clock) {
        this.turnos = Objects.requireNonNull(turnos, "turnos es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.consultas = Objects.requireNonNull(consultas, "consultas es obligatorio");
        this.identifiers = Objects.requireNonNull(identifiers, "identifiers es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> execute(AbrirTurnoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var terminal = referencias.terminal(command.tenantId(), command.terminalId());
        if (terminal.isEmpty()) return Result.failure(VentasErrors.terminalNoEncontrada());
        if (!terminal.get().operable()) return Result.failure(VentasErrors.terminalNoOperable());
        if (consultas.actual(new TurnoActualQuery(command.tenantId(), command.terminalId())).isSuccess()) {
            return Result.failure(VentasErrors.turnoYaAbierto());
        }
        return TurnoCaja.abrir(
                        identifiers.next(), command.tenantId(), terminal.get().id(),
                        terminal.get().establecimientoId(), new Actor(command.actorId()), command.fondoInicial(),
                        clock.now())
                .fold(
                        this::persistir,
                        error -> Result.<TurnoResult, ApplicationError>failure(VentasErrors.fromDomain(error)));
    }

    private Result<TurnoResult, ApplicationError> persistir(TurnoCaja turno) {
        if (turnos.insertar(turno) == GuardadoOutcome.DUPLICADO) {
            return Result.failure(VentasErrors.turnoYaAbierto());
        }
        return consultas.obtener(new ObtenerTurnoQuery(turno.tenantId(), turno.id()));
    }
}
