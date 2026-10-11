package com.softprimesolutions.inventario.application.dto.command;

import java.util.UUID;

public record BloquearLoteCommand(UUID tenantId, UUID loteId, String motivo, UUID actorId) {
}
