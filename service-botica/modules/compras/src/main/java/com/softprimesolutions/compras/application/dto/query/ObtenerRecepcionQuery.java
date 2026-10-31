package com.softprimesolutions.compras.application.dto.query;

import java.util.UUID;

public record ObtenerRecepcionQuery(UUID tenantId, UUID recepcionId) {
}
