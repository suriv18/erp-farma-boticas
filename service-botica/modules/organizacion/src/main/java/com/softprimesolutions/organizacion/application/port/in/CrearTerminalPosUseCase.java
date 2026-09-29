package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CrearTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearTerminalPosUseCase {
    Result<TerminalPosResult, ApplicationError> execute(CrearTerminalPosCommand command);
}
