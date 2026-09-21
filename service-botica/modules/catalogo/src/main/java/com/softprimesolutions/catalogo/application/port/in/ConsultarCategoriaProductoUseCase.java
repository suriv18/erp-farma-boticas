package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCategoriaProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarCategoriaProductoUseCase {
    Result<CategoriaProductoResult, ApplicationError> execute(ConsultarCategoriaProductoQuery query);
}
