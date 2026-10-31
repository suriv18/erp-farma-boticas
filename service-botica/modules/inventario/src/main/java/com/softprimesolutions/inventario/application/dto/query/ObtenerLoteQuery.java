package com.softprimesolutions.inventario.application.dto.query;

import java.util.UUID;

public record ObtenerLoteQuery(UUID tenantId, UUID loteId) {
}
