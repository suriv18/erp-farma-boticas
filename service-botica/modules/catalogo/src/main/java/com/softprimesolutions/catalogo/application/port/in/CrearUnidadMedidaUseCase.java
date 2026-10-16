package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearUnidadMedidaCommand;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearUnidadMedidaUseCase {
    Result<UnidadMedidaResult, ApplicationError> execute(CrearUnidadMedidaCommand command);
}
