package com.softprimesolutions.organizacion.application.usecase.query;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerTerminalQuery;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.port.in.ObtenerTerminalUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerTerminalHandler implements ObtenerTerminalUseCase {

    private final OrganizacionReadPort readPort;

    public ObtenerTerminalHandler(OrganizacionReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<TerminalPosResult, ApplicationError> execute(ObtenerTerminalQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findTerminalById(query.tenantId(), query.terminalId())
                .<Result<TerminalPosResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "ORG_TERMINAL_NO_ENCONTRADO", "El terminal indicado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
