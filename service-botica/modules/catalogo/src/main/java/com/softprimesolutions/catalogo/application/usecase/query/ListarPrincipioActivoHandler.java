package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarPrincipioActivoQuery;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.port.in.ListarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarPrincipioActivoHandler implements ListarPrincipioActivoUseCase {

    private final CatalogoReadPort readPort;

    public ListarPrincipioActivoHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<PrincipioActivoResult>, ApplicationError> execute(ListarPrincipioActivoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findPrincipiosActivos(query.texto(), query.estado()));
    }
}
