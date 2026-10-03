package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import java.util.Optional;
import java.util.UUID;

public interface VentasReadPort {

    Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId);

    Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId);
}
