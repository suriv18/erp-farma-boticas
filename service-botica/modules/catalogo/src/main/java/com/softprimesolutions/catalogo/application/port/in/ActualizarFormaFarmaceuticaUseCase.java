package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarFormaFarmaceuticaCommand;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarFormaFarmaceuticaUseCase {
    Result<FormaFarmaceuticaResult, ApplicationError> execute(ActualizarFormaFarmaceuticaCommand command);
}
