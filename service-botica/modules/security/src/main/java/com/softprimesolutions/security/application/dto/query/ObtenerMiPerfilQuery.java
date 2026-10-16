package com.softprimesolutions.security.application.dto.query;

import java.util.UUID;

public record ObtenerMiPerfilQuery(UUID tenantId, UUID userId) {
}
