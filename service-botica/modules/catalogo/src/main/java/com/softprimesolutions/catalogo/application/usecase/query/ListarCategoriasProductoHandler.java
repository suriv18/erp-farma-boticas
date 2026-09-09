package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ListarCategoriasProductoQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.port.in.ListarCategoriasProductoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.List;
import java.util.Objects;

public final class ListarCategoriasProductoHandler implements ListarCategoriasProductoUseCase {

    private final CatalogoReadPort readPort;

    public ListarCategoriasProductoHandler(CatalogoReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<List<CategoriaProductoResult>, ApplicationError> execute(ListarCategoriasProductoQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return Result.success(readPort.findCategoriasProducto(
                query.tenantId(), query.categoriaPadreId(), query.estado()));
    }
}
