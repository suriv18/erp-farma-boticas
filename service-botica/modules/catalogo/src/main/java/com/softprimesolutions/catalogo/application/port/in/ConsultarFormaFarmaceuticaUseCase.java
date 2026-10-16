package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarFormaFarmaceuticaQuery;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarFormaFarmaceuticaUseCase {
    Result<FormaFarmaceuticaResult, ApplicationError> execute(ConsultarFormaFarmaceuticaQuery query);
}
