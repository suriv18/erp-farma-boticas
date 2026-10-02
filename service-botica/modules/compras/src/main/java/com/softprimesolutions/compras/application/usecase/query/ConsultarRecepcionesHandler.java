package com.softprimesolutions.compras.application.usecase.query;

import com.softprimesolutions.compras.application.dto.query.ObtenerRecepcionQuery;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.port.in.ConsultarRecepcionesUseCase;
import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarRecepcionesHandler implements ConsultarRecepcionesUseCase {

    private final ComprasReadPort readPort;

    public ConsultarRecepcionesHandler(ComprasReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<RecepcionResult, ApplicationError> obtener(ObtenerRecepcionQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findRecepcion(query.tenantId(), query.recepcionId())
                .<Result<RecepcionResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(ComprasErrors.recepcionNoEncontrada()));
    }
}
