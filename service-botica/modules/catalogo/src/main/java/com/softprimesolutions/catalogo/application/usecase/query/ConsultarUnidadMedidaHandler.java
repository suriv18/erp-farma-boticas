package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarUnidadMedidaQuery;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarUnidadMedidaHandler implements ConsultarUnidadMedidaUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarUnidadMedidaHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<UnidadMedidaResult, ApplicationError> execute(ConsultarUnidadMedidaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findUnidadMedidaByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<UnidadMedidaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_UNIDAD_MEDIDA_NO_ENCONTRADA", "La unidad de medida indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
