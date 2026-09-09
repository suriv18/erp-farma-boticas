package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarPrincipioActivoUseCase {
    Result<PrincipioActivoResult, ApplicationError> execute(ActualizarPrincipioActivoCommand command);
}
