package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearViaAdministracionCommand;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearViaAdministracionUseCase {
    Result<ViaAdministracionResult, ApplicationError> execute(CrearViaAdministracionCommand command);
}
