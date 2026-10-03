package com.softprimesolutions.inventario.application.dto.command;

import java.util.UUID;

public record ReintegrarSalidasDeVentaCommand(UUID tenantId, UUID ventaId, UUID actorId) {
}
