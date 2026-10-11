package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ListarEstablecimientosQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarEstablecimientosUseCase {
    Result<PaginaResult<EstablecimientoResult>, ApplicationError> execute(ListarEstablecimientosQuery query);
}
