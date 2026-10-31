package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarProductoReguladoQuery;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarProductoReguladoUseCase {
    Result<ProductoReguladoResult, ApplicationError> execute(ConsultarProductoReguladoQuery query);
}
