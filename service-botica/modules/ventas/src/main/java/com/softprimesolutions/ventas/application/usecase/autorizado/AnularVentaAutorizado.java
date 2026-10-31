package com.softprimesolutions.ventas.application.usecase.autorizado;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AnularVentaCommand;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort.Ambito;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Objects;

public final class AnularVentaAutorizado implements AnularVentaUseCase {

    static final String PERMISO = "ventas.ventas.anular";

    private final AnularVentaUseCase delegado;
    private final VentasReadPort lectura;
    private final ControlAmbito control;

    public AnularVentaAutorizado(AnularVentaUseCase delegado, VentasReadPort lectura, ControlAmbito control) {
        this.delegado = Objects.requireNonNull(delegado, "delegado es obligatorio");
        this.lectura = Objects.requireNonNull(lectura, "lectura es obligatoria");
        this.control = Objects.requireNonNull(control, "control es obligatorio");
    }

    @Override
    public Result<VentaResult, ApplicationError> execute(AnularVentaCommand command) {
        return lectura.findVenta(command.tenantId(), command.ventaId())
                .map(venta -> control.antes(
                        new Ambito(command.tenantId(), command.actorId(), PERMISO, venta.establecimientoId(), null,
                                venta.terminalId()),
                        () -> delegado.execute(command)))
                .orElseGet(() -> delegado.execute(command));
    }
}
