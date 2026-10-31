package com.softprimesolutions.ventas.application.usecase.query;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.Objects;
import java.util.Optional;

public final class ConsultarTurnosHandler implements ConsultarTurnosUseCase {

    private final VentasReadPort readPort;

    public ConsultarTurnosHandler(VentasReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<TurnoResult, ApplicationError> obtener(ObtenerTurnoQuery query) {
        Objects.requireNonNull(query, "query es obligatoria");
        return resultado(readPort.findTurno(query.tenantId(), query.turnoId()));
    }

    @Override
    public Result<TurnoResult, ApplicationError> actual(TurnoActualQuery query) {
        Objects.requireNonNull(query, "query es obligatoria");
        return resultado(readPort.findTurnoAbierto(query.tenantId(), query.terminalId()));
    }

    private static Result<TurnoResult, ApplicationError> resultado(Optional<TurnoResult> turno) {
        return turno.<Result<TurnoResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(VentasErrors.turnoNoEncontrado()));
    }
}
