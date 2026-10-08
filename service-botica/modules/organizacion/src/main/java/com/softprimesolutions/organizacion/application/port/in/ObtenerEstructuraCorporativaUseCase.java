package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerEstructuraCorporativaUseCase {
    Result<EstructuraCorporativaResult, ApplicationError> execute(ObtenerEstructuraCorporativaQuery query);
}
