package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerMiPuestoQuery;
import com.softprimesolutions.organizacion.application.dto.result.MiPuestoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerMiPuestoUseCase {
    Result<MiPuestoResult, ApplicationError> execute(ObtenerMiPuestoQuery query);
}
