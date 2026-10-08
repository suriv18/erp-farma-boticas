package com.softprimesolutions.inventario.application.usecase.query;

import com.softprimesolutions.inventario.application.dto.query.ListarPosicionesQuery;
import com.softprimesolutions.inventario.application.dto.result.PaginaResult;
import com.softprimesolutions.inventario.application.dto.result.PosicionResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.port.in.ListarPosicionesUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ListarPosicionesHandler implements ListarPosicionesUseCase {

    private final InventarioReadPort readPort;

    public ListarPosicionesHandler(InventarioReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PaginaResult<PosicionResult>, ApplicationError> execute(ListarPosicionesQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            return Result.failure(InventarioErrors.paginacionInvalida(query.page(), query.size()));
        }
        return Result.success(readPort.findPosiciones(
                query.tenantId(), query.establecimientoId(), query.almacenId(), query.skuId(),
                query.page(), query.size()));
    }
}
