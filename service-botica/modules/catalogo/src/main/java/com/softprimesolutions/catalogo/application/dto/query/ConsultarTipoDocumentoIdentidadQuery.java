package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarTipoDocumentoIdentidadQuery(String codigo) implements Query<TipoDocumentoIdentidadResult> {
}
