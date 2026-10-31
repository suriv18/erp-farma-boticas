package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ConsultarClasificacionControladaQuery(String codigo) implements Query<ClasificacionControladaResult> {
}
