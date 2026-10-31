package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;

@FunctionalInterface
public interface AbrirTurnoUseCase {
    Result<TurnoResult, ApplicationError> execute(AbrirTurnoCommand command);
}
