package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.query.ObtenerRolQuery;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerRolUseCase {
    Result<RolResult, ApplicationError> execute(ObtenerRolQuery query);
}
