package com.softprimesolutions.security.application.usecase.query;

import com.softprimesolutions.security.application.dto.query.ListarPermisosQuery;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.port.in.ListarPermisosUseCase;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarPermisosHandler implements ListarPermisosUseCase {

    private final IamReadPort readPort;

    public ListarPermisosHandler(IamReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<PermisoResult>, ApplicationError> execute(ListarPermisosQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findPermissions(query.search()));
    }
}
