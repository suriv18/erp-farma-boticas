package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarTipoDocumentoIdentidadUseCase {
    Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ConsultarTipoDocumentoIdentidadQuery query);
}
