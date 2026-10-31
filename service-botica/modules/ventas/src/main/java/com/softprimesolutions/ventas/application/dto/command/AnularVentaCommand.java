package com.softprimesolutions.ventas.application.dto.command;

import java.util.UUID;

public record AnularVentaCommand(UUID tenantId, UUID actorId, UUID ventaId, String motivo) {
}
