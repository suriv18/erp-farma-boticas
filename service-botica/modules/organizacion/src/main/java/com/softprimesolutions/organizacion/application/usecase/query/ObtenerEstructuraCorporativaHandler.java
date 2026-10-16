package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstructuraCorporativaUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerEstructuraCorporativaHandler implements ObtenerEstructuraCorporativaUseCase {

    private final OrganizacionReadPort readPort;

    public ObtenerEstructuraCorporativaHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<EstructuraCorporativaResult, ApplicationError> execute(
            ObtenerEstructuraCorporativaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findEstructuraCorporativa(query.tenantId()));
    }
}
