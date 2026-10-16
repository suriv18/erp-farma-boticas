package com.softprimesolutions.ventas.application.port.in;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;

@FunctionalInterface
public interface CerrarTurnoUseCase {
    Result<TurnoResult, ApplicationError> execute(CerrarTurnoCommand command);
}
