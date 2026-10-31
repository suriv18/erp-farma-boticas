package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerAlmacenQuery(UUID tenantId, UUID almacenId) {
}
