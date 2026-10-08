package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ListarTiposDocumentoIdentidadQuery(String estado, int page, int size)
        implements Query<PaginaResult<TipoDocumentoIdentidadResult>> {
}
