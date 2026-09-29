package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ListarAlmacenesQuery(UUID tenantId, UUID establecimientoId, String search, int page, int size) {
}
