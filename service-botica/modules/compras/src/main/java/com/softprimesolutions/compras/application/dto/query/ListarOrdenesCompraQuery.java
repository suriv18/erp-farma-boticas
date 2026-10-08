package com.softprimesolutions.compras.application.dto.query;

import java.util.UUID;

public record ListarOrdenesCompraQuery(UUID tenantId, UUID proveedorId, String estado, int page, int size) {
}
