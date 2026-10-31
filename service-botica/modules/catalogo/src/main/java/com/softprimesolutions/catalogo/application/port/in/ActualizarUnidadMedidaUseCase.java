package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarUnidadMedidaCommand;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarUnidadMedidaUseCase {
    Result<UnidadMedidaResult, ApplicationError> execute(ActualizarUnidadMedidaCommand command);
}
