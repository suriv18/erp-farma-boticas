package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarClasificacionesControladasQuery;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarClasificacionesControladasUseCase {
    Result<List<ClasificacionControladaResult>, ApplicationError> execute(ListarClasificacionesControladasQuery query);
}
