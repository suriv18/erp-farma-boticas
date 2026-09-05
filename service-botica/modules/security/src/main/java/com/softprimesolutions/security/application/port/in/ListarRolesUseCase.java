package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.query.ListarRolesQuery;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarRolesUseCase {
    Result<PaginaResult<RolResult>, ApplicationError> execute(ListarRolesQuery query);
}
