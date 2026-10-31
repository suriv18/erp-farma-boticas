package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarViasAdministracionQuery;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.port.in.ListarViasAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarViasAdministracionHandler implements ListarViasAdministracionUseCase {

    private final CatalogoReadPort readPort;

    public ListarViasAdministracionHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<ViaAdministracionResult>, ApplicationError> execute(ListarViasAdministracionQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findViasAdministracion(query.estado()));
    }
}
