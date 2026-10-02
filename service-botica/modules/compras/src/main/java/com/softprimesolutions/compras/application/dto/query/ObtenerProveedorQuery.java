package com.softprimesolutions.compras.application.dto.query;

import java.util.UUID;

public record ObtenerProveedorQuery(UUID tenantId, UUID proveedorId) {
}
