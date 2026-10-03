package com.softprimesolutions.ventas.application.port.out;

import java.util.Optional;
import java.util.UUID;

public interface ReferenciasVentasPort {

    Optional<TerminalRef> terminal(UUID tenantId, UUID terminalId);

    record TerminalRef(UUID id, UUID establecimientoId, String codigo, boolean operable) {
    }
}
