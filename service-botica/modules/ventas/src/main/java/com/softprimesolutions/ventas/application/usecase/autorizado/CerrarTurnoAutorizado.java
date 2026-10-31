package com.softprimesolutions.ventas.application.usecase.autorizado;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Objects;

public final class CerrarTurnoAutorizado implements CerrarTurnoUseCase {

    static final String PERMISO = "ventas.turnos.cerrar";

    private final CerrarTurnoUseCase delegado;
    private final VentasReadPort lectura;
    private final ControlAmbito control;

    public CerrarTurnoAutorizado(CerrarTurnoUseCase delegado, VentasReadPort lectura, ControlAmbito control) {
        this.delegado = Objects.requireNonNull(delegado, "delegado es obligatorio");
        this.lectura = Objects.requireNonNull(lectura, "lectura es obligatoria");
        this.control = Objects.requireNonNull(control, "control es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> execute(CerrarTurnoCommand command) {
        return lectura.findTurno(command.tenantId(), command.turnoId())
                .map(turno -> control.antes(
                        new Ambito(command.tenantId(), command.actorId(), PERMISO, turno.establecimientoId(), null,
                                turno.terminalId()),
                        () -> delegado.execute(command)))
                .orElseGet(() -> delegado.execute(command));
    }
}
