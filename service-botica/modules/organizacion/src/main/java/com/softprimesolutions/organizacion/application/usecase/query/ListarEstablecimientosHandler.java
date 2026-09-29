package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ListarEstablecimientosQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.port.in.ListarEstablecimientosUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class ListarEstablecimientosHandler implements ListarEstablecimientosUseCase {

    private final OrganizacionReadPort readPort;

    public ListarEstablecimientosHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PaginaResult<EstablecimientoResult>, ApplicationError> execute(
            ListarEstablecimientosQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            return Result.failure(new StandardApplicationError(
                    "ORG_PAGINACION_INVALIDA",
                    "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                    ErrorCategory.VALIDATION,
                    Map.of("page", query.page(), "size", query.size())));
        }
        return Result.success(readPort.findEstablecimientos(
                query.tenantId(), query.empresaId(), query.search(), query.page(), query.size()));
    }
}
