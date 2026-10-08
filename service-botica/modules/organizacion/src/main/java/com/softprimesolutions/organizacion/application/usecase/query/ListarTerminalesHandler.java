package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ListarTerminalesQuery;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.port.in.ListarTerminalesUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class ListarTerminalesHandler implements ListarTerminalesUseCase {

    private final OrganizacionReadPort readPort;

    public ListarTerminalesHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PaginaResult<TerminalPosResult>, ApplicationError> execute(ListarTerminalesQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            return Result.failure(new StandardApplicationError(
                    "ORG_PAGINACION_INVALIDA",
                    "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                    ErrorCategory.VALIDATION,
                    Map.of("page", query.page(), "size", query.size())));
        }
        return Result.success(readPort.findTerminales(
                query.tenantId(), query.establecimientoId(), query.search(), query.page(), query.size()));
    }
}
