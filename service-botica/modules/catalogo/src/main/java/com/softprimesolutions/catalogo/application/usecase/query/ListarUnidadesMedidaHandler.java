package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarUnidadesMedidaQuery;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.port.in.ListarUnidadesMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarUnidadesMedidaHandler implements ListarUnidadesMedidaUseCase {

    private final CatalogoReadPort readPort;

    public ListarUnidadesMedidaHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<UnidadMedidaResult>, ApplicationError> execute(ListarUnidadesMedidaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findUnidadesMedida(query.estado()));
    }
}
