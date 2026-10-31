package com.softprimesolutions.security.application.usecase.query;

import com.softprimesolutions.security.application.dto.query.ObtenerRolQuery;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.port.in.ObtenerRolUseCase;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerRolHandler implements ObtenerRolUseCase {

    private final IamReadPort readPort;

    public ObtenerRolHandler(IamReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<RolResult, ApplicationError> execute(ObtenerRolQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findRoleById(query.tenantId(), query.roleId())
                .map(Result::<RolResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
