package com.softprimesolutions.inventario.application.usecase.query;

import com.softprimesolutions.inventario.application.dto.query.ObtenerLoteQuery;
import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.error.InventarioErrors;
import com.softprimesolutions.inventario.application.port.in.ObtenerLoteUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerLoteHandler implements ObtenerLoteUseCase {

    private final InventarioReadPort readPort;

    public ObtenerLoteHandler(InventarioReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<LoteResult, ApplicationError> execute(ObtenerLoteQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findLoteById(query.tenantId(), query.loteId())
                .<Result<LoteResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(InventarioErrors.loteNoEncontrado()));
    }
}
