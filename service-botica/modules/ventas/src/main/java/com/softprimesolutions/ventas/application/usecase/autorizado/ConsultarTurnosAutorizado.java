package com.softprimesolutions.ventas.application.usecase.autorizado;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.ActorActualPort;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import java.util.Objects;

public final class ConsultarTurnosAutorizado implements ConsultarTurnosUseCase {

    static final String PERMISO = "ventas.turnos.consultar";

    private final ConsultarTurnosUseCase delegado;
    private final ReferenciasVentasPort referencias;
    private final ActorActualPort actor;
    private final ControlAmbito control;

    public ConsultarTurnosAutorizado(
            ConsultarTurnosUseCase delegado, ReferenciasVentasPort referencias, ActorActualPort actor,
            ControlAmbito control) {
        this.delegado = Objects.requireNonNull(delegado, "delegado es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.actor = Objects.requireNonNull(actor, "actor es obligatorio");
        this.control = Objects.requireNonNull(control, "control es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> obtener(ObtenerTurnoQuery query) {
        return control.despues(
                delegado.obtener(query),
                turno -> new Ambito(
                        query.tenantId(), actor.actorId(), PERMISO, turno.establecimientoId(), null,
                        turno.terminalId()));
    }

    @Override
    public Result<TurnoResult, ApplicationError> actual(TurnoActualQuery query) {
        return referencias.terminal(query.tenantId(), query.terminalId())
                .map(terminal -> control.antes(
                        new Ambito(query.tenantId(), actor.actorId(), PERMISO, terminal.establecimientoId(), null,
                                terminal.id()),
                        () -> delegado.actual(query)))
                .orElseGet(() -> delegado.actual(query));
    }
}
