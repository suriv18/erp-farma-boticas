package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;

public record ListarClasificacionesControladasQuery(String estado)
        implements Query<List<ClasificacionControladaResult>> {
}
