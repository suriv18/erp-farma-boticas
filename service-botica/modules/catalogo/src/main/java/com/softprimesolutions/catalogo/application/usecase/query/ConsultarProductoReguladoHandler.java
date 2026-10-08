package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarProductoReguladoQuery;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarProductoReguladoUseCase;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarProductoReguladoHandler implements ConsultarProductoReguladoUseCase {

    private final ProductoReguladoPort productoReguladoPort;

    public ConsultarProductoReguladoHandler(ProductoReguladoPort productoReguladoPort) {
        this.productoReguladoPort = Objects.requireNonNull(productoReguladoPort, "productoReguladoPort es obligatorio");
    }

    @Override
    public Result<ProductoReguladoResult, ApplicationError> execute(ConsultarProductoReguladoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return productoReguladoPort.findById(query.productoReguladoId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<ProductoReguladoResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_PRODUCTO_REGULADO_NO_ENCONTRADO", "El producto regulado indicado no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
