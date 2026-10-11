package com.softprimesolutions.inventario.application.dto.command;

import java.util.UUID;

public record DesbloquearLoteCommand(UUID tenantId, UUID loteId, UUID actorId) {
}
