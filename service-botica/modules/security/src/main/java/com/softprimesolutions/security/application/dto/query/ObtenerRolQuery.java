package com.softprimesolutions.security.application.dto.query;

import java.util.UUID;

public record ObtenerRolQuery(UUID tenantId, UUID roleId) {
}
