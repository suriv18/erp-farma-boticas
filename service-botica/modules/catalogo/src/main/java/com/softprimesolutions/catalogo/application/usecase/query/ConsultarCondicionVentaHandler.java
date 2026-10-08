package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCondicionVentaQuery;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarCondicionVentaHandler implements ConsultarCondicionVentaUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarCondicionVentaHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<CondicionVentaResult, ApplicationError> execute(ConsultarCondicionVentaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findCondicionVentaByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<CondicionVentaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_CONDICION_VENTA_NO_ENCONTRADA", "La condición de venta indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
