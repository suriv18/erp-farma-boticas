package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstablecimientoQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerEstablecimientoHandler implements ObtenerEstablecimientoUseCase {

    private final OrganizacionReadPort readPort;

    public ObtenerEstablecimientoHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<EstablecimientoResult, ApplicationError> execute(ObtenerEstablecimientoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findEstablecimientoById(query.tenantId(), query.establecimientoId())
                .<Result<EstablecimientoResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "ORG_ESTABLECIMIENTO_NO_ENCONTRADO", "El establecimiento indicado no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
