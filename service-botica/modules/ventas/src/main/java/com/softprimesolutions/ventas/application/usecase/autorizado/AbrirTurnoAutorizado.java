package com.softprimesolutions.ventas.application.usecase.autorizado;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import java.util.Objects;

public final class AbrirTurnoAutorizado implements AbrirTurnoUseCase {

    static final String PERMISO = "ventas.turnos.abrir";

    private final AbrirTurnoUseCase delegado;
    private final ReferenciasVentasPort referencias;
    private final ControlAmbito control;

    public AbrirTurnoAutorizado(
            AbrirTurnoUseCase delegado, ReferenciasVentasPort referencias, ControlAmbito control) {
        this.delegado = Objects.requireNonNull(delegado, "delegado es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.control = Objects.requireNonNull(control, "control es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> execute(AbrirTurnoCommand command) {
        return referencias.terminal(command.tenantId(), command.terminalId())
                .map(terminal -> control.antes(
                        new Ambito(command.tenantId(), command.actorId(), PERMISO, terminal.establecimientoId(),
                                null, terminal.id()),
                        () -> delegado.execute(command)))
                .orElseGet(() -> delegado.execute(command));
    }
}
