package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarCategoriasProductoUseCase {
    Result<List<CategoriaProductoResult>, ApplicationError> execute(ListarCategoriasProductoQuery query);
}
