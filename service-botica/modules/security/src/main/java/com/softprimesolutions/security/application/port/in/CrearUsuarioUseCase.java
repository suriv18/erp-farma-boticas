package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.command.CrearUsuarioCommand;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearUsuarioUseCase {
    Result<UsuarioResult, ApplicationError> execute(CrearUsuarioCommand command);
}
