package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarFormaFarmaceuticaQuery;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarFormaFarmaceuticaHandler implements ConsultarFormaFarmaceuticaUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarFormaFarmaceuticaHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<FormaFarmaceuticaResult, ApplicationError> execute(ConsultarFormaFarmaceuticaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findFormaFarmaceuticaByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<FormaFarmaceuticaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_FORMA_FARMACEUTICA_NO_ENCONTRADA", "La forma farmacéutica indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
