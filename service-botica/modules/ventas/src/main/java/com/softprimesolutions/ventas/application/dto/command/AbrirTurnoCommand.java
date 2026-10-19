package com.softprimesolutions.ventas.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record AbrirTurnoCommand(UUID tenantId, UUID actorId, UUID terminalId, BigDecimal fondoInicial) {
}
