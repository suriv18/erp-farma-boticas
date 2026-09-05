package com.softprimesolutions.security.application.port.in;

import com.softprimesolutions.security.application.dto.query.ListarPermisosQuery;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;

@FunctionalInterface
public interface ListarPermisosUseCase {
    Result<List<PermisoResult>, ApplicationError> execute(ListarPermisosQuery query);
}
