package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarClasificacionControladaQuery;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarClasificacionControladaHandler implements ConsultarClasificacionControladaUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarClasificacionControladaHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<ClasificacionControladaResult, ApplicationError> execute(
            ConsultarClasificacionControladaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findClasificacionControladaByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<ClasificacionControladaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_CLASIFICACION_CONTROLADA_NO_ENCONTRADA",
                        "La clasificación controlada indicada no existe.", ErrorCategory.NOT_FOUND)));
    }
}
