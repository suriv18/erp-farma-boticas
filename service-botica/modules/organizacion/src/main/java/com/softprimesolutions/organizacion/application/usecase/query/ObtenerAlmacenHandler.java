package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerAlmacenQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerAlmacenHandler implements ObtenerAlmacenUseCase {

    private final OrganizacionReadPort readPort;

    public ObtenerAlmacenHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<AlmacenResult, ApplicationError> execute(ObtenerAlmacenQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findAlmacenById(query.tenantId(), query.almacenId())
                .<Result<AlmacenResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "ORG_ALMACEN_NO_ENCONTRADO", "El almacén indicado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
