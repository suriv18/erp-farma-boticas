package com.softprimesolutions.ventas.application.dto.query;

import java.util.UUID;

public record ObtenerVentaQuery(UUID tenantId, UUID ventaId) {
}
