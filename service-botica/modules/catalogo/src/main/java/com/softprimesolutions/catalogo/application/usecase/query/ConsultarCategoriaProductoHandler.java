package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCategoriaProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarCategoriaProductoHandler implements ConsultarCategoriaProductoUseCase {

    private final CatalogoComercialPort comercialPort;

    public ConsultarCategoriaProductoHandler(CatalogoComercialPort comercialPort) {
        this.comercialPort = Objects.requireNonNull(comercialPort, "comercialPort es obligatorio");
    }

    @Override
    public Result<CategoriaProductoResult, ApplicationError> execute(ConsultarCategoriaProductoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return comercialPort.findCategoriaById(query.tenantId(), query.categoriaId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<CategoriaProductoResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_CATEGORIA_PRODUCTO_NO_ENCONTRADA", "La categoría indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
