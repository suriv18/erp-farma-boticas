package com.softprimesolutions.security.application.usecase.query;

import com.softprimesolutions.security.application.dto.query.ListarRolesQuery;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.port.in.ListarRolesUseCase;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class ListarRolesHandler implements ListarRolesUseCase {

    private final IamReadPort readPort;

    public ListarRolesHandler(IamReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PaginaResult<RolResult>, ApplicationError> execute(ListarRolesQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            return Result.failure(new StandardApplicationError(
                    "SEC_PAGINACION_INVALIDA",
                    "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                    ErrorCategory.VALIDATION,
                    Map.of("page", query.page(), "size", query.size())));
        }
        return Result.success(readPort.findRoles(query.tenantId(), query.search(), query.page(), query.size()));
    }
}
