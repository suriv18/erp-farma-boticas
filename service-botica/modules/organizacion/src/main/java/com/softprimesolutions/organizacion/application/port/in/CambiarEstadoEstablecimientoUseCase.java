package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CambiarEstadoEstablecimientoUseCase {

    Result<EstablecimientoResult, ApplicationError> execute(CambiarEstadoEstablecimientoCommand command);
}
