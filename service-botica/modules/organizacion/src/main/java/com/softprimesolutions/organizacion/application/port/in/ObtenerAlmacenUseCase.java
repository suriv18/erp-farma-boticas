package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerAlmacenQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerAlmacenUseCase {
    Result<AlmacenResult, ApplicationError> execute(ObtenerAlmacenQuery query);
}
