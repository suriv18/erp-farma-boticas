package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarTiposDocumentoIdentidadUseCase {
    Result<PaginaResult<TipoDocumentoIdentidadResult>, ApplicationError> execute(
            ListarTiposDocumentoIdentidadQuery query);
}
