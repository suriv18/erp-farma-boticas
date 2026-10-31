package com.softprimesolutions.ventas.application.port.out;

import java.util.UUID;

@FunctionalInterface
public interface NumeracionPort {

    String siguienteNumeroOperacion(UUID tenantId, UUID terminalId);
}
