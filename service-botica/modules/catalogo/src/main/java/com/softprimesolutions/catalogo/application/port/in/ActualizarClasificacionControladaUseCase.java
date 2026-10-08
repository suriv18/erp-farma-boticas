package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarClasificacionControladaCommand;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarClasificacionControladaUseCase {
    Result<ClasificacionControladaResult, ApplicationError> execute(ActualizarClasificacionControladaCommand command);
}
