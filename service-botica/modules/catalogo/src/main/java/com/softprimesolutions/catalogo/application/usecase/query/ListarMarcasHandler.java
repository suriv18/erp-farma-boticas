package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarMarcasQuery;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.port.in.ListarMarcasUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarMarcasHandler implements ListarMarcasUseCase {

    private final CatalogoReadPort readPort;

    public ListarMarcasHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<MarcaResult>, ApplicationError> execute(ListarMarcasQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findMarcas(query.tenantId(), query.estado()));
    }
}
