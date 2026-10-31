package com.softprimesolutions.compras.application.dto.command;

import java.util.UUID;

public record CrearProveedorCommand(UUID tenantId, UUID actorId, ProveedorInput proveedor) {
}
