package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarViaAdministracionCommand;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarViaAdministracionUseCase {
    Result<ViaAdministracionResult, ApplicationError> execute(ActualizarViaAdministracionCommand command);
}
