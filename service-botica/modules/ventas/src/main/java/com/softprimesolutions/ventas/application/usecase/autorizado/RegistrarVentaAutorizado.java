package com.softprimesolutions.ventas.application.usecase.autorizado;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import java.util.Objects;

public final class RegistrarVentaAutorizado implements RegistrarVentaUseCase {

    static final String PERMISO = "ventas.ventas.registrar";

    private final RegistrarVentaUseCase delegado;
    private final ReferenciasVentasPort referencias;
    private final ControlAmbito control;

    public RegistrarVentaAutorizado(
            RegistrarVentaUseCase delegado, ReferenciasVentasPort referencias, ControlAmbito control) {
        this.delegado = Objects.requireNonNull(delegado, "delegado es obligatorio");
        this.referencias = Objects.requireNonNull(referencias, "referencias es obligatorio");
        this.control = Objects.requireNonNull(control, "control es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> execute(RegistrarVentaCommand command) {
        return referencias.terminal(command.tenantId(), command.terminalId())
                .map(terminal -> control.antes(
                        new Ambito(command.tenantId(), command.actorId(), PERMISO, terminal.establecimientoId(),
                                command.almacenId(), terminal.id()),
                        () -> delegado.execute(command)))
                .orElseGet(() -> delegado.execute(command));
    }
}
