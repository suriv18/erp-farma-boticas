package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarClasificacionesControladasQuery;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.port.in.ListarClasificacionesControladasUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarClasificacionesControladasHandler implements ListarClasificacionesControladasUseCase {

    private final CatalogoReadPort readPort;

    public ListarClasificacionesControladasHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<ClasificacionControladaResult>, ApplicationError> execute(
            ListarClasificacionesControladasQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findClasificacionesControladas(query.estado()));
    }
}
