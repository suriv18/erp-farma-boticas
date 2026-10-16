package com.softprimesolutions.inventario.application.usecase.command;

import com.softprimesolutions.inventario.application.dto.command.DesbloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.port.in.DesbloquearLoteUseCase;
import com.softprimesolutions.inventario.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class DesbloquearLoteHandler implements DesbloquearLoteUseCase {

    private final CambioEstadoLote cambioEstado;

    public DesbloquearLoteHandler(CambioEstadoLote cambioEstado) {
        this.cambioEstado = Objects.requireNonNull(cambioEstado, "cambioEstado es obligatorio");
    }

    @Override
    public Result<LoteResult, ApplicationError> execute(DesbloquearLoteCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return cambioEstado.aplicar(command.tenantId(), command.loteId(),
                (lote, ahora, hoy) -> lote.desbloquear(new Actor(command.actorId()), hoy, ahora));
    }
}
