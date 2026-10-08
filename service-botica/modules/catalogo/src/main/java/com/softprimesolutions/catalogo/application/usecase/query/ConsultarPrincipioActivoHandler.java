package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarPrincipioActivoHandler implements ConsultarPrincipioActivoUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarPrincipioActivoHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<PrincipioActivoResult, ApplicationError> execute(ConsultarPrincipioActivoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findPrincipioActivoById(query.principioActivoId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<PrincipioActivoResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO", "El principio activo indicado no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
