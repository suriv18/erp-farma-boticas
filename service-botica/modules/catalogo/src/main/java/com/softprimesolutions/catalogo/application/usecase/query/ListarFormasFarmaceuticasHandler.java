package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarFormasFarmaceuticasQuery;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.port.in.ListarFormasFarmaceuticasUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarFormasFarmaceuticasHandler implements ListarFormasFarmaceuticasUseCase {

    private final CatalogoReadPort readPort;

    public ListarFormasFarmaceuticasHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<FormaFarmaceuticaResult>, ApplicationError> execute(ListarFormasFarmaceuticasQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findFormasFarmaceuticas(query.estado()));
    }
}
