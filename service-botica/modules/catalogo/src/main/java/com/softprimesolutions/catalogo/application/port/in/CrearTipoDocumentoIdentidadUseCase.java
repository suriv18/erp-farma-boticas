package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.CrearTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearTipoDocumentoIdentidadUseCase {
    Result<TipoDocumentoIdentidadResult, ApplicationError> execute(CrearTipoDocumentoIdentidadCommand command);
}
