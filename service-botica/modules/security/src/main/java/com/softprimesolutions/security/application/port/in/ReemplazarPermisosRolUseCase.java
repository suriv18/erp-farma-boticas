package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.command.ReemplazarPermisosRolCommand;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ReemplazarPermisosRolUseCase {
    Result<RolResult, ApplicationError> execute(ReemplazarPermisosRolCommand command);
}
