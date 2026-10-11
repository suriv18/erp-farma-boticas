package com.softprimesolutions.compras.application.dto.query;

import java.util.UUID;

public record ObtenerOrdenCompraQuery(UUID tenantId, UUID ordenId) {
}
