package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarClasificacionControladaQuery;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarClasificacionControladaUseCase {
    Result<ClasificacionControladaResult, ApplicationError> execute(ConsultarClasificacionControladaQuery query);
}
