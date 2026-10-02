package com.softprimesolutions.inventario.application.port.in;

import com.softprimesolutions.inventario.application.dto.query.ListarPosicionesQuery;
import com.softprimesolutions.inventario.application.dto.result.PaginaResult;
import com.softprimesolutions.inventario.application.dto.result.PosicionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarPosicionesUseCase {
    Result<PaginaResult<PosicionResult>, ApplicationError> execute(ListarPosicionesQuery query);
}
