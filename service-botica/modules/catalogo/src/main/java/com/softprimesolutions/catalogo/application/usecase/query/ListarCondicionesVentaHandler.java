package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarCondicionesVentaQuery;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.port.in.ListarCondicionesVentaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarCondicionesVentaHandler implements ListarCondicionesVentaUseCase {

    private final CatalogoReadPort readPort;

    public ListarCondicionesVentaHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<CondicionVentaResult>, ApplicationError> execute(ListarCondicionesVentaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findCondicionesVenta(query.estado()));
    }
}
