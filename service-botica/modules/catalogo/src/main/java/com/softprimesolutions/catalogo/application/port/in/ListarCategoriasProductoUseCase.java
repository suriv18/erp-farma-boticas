package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarCategoriasProductoUseCase {
    Result<PaginaResult<CategoriaProductoResult>, ApplicationError> execute(ListarCategoriasProductoQuery query);
}
