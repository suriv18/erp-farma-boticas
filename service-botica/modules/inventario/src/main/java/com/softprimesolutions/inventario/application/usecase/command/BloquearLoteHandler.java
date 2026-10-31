package com.softprimesolutions.inventario.application.usecase.command;

import com.softprimesolutions.inventario.application.dto.command.BloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.port.in.BloquearLoteUseCase;
import com.softprimesolutions.inventario.domain.valueobject.Actor;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class BloquearLoteHandler implements BloquearLoteUseCase {

    private final CambioEstadoLote cambioEstado;

    public BloquearLoteHandler(CambioEstadoLote cambioEstado) {
        this.cambioEstado = Objects.requireNonNull(cambioEstado, "cambioEstado es obligatorio");
    }

    @Override
    public Result<LoteResult, ApplicationError> execute(BloquearLoteCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return cambioEstado.aplicar(command.tenantId(), command.loteId(),
                (lote, ahora, hoy) -> lote.bloquear(command.motivo(), new Actor(command.actorId()), ahora));
    }
}
