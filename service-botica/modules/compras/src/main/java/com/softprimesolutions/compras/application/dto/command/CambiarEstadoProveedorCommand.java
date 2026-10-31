package com.softprimesolutions.compras.application.dto.command;

import java.util.UUID;

public record CambiarEstadoProveedorCommand(UUID tenantId, UUID proveedorId, String estado, UUID actorId) {
}
