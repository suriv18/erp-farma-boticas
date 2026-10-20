package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerMiPuestoQuery(UUID tenantId, UUID usuarioId) {
}
