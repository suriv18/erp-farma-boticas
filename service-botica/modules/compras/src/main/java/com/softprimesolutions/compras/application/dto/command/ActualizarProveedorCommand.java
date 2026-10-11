package com.softprimesolutions.compras.application.dto.command;

import java.util.UUID;

public record ActualizarProveedorCommand(UUID tenantId, UUID proveedorId, UUID actorId, ProveedorInput proveedor) {
}
