package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarTiposDocumentoIdentidadUseCase {
    Result<List<TipoDocumentoIdentidadResult>, ApplicationError> execute(ListarTiposDocumentoIdentidadQuery query);
}
