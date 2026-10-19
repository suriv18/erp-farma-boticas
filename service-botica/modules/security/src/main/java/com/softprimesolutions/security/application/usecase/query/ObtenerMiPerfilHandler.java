package com.softprimesolutions.security.application.usecase.query;

import com.softprimesolutions.security.application.dto.query.ObtenerMiPerfilQuery;
import com.softprimesolutions.security.application.dto.result.PerfilResult;
import com.softprimesolutions.security.application.port.in.ObtenerMiPerfilUseCase;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerMiPerfilHandler implements ObtenerMiPerfilUseCase {

    private final IamReadPort readPort;

    public ObtenerMiPerfilHandler(IamReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PerfilResult, ApplicationError> execute(ObtenerMiPerfilQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findProfile(query.tenantId(), query.userId())
                .map(Result::<PerfilResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "SEC_USUARIO_NO_ENCONTRADO", "El usuario autenticado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
