package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarTipoDocumentoIdentidadUseCase {
    Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ActualizarTipoDocumentoIdentidadCommand command);
}
