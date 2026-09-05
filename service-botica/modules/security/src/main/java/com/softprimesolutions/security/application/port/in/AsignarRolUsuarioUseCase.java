package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.command.AsignarRolUsuarioCommand;
import com.softprimesolutions.security.application.dto.result.AsignacionRolResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface AsignarRolUsuarioUseCase {
    Result<AsignacionRolResult, ApplicationError> execute(AsignarRolUsuarioCommand command);
}
