package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarPrincipioActivoUseCase {
    Result<PrincipioActivoResult, ApplicationError> execute(ConsultarPrincipioActivoQuery query);
}
