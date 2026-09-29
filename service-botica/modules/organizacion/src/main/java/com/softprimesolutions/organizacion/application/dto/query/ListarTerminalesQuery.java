package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ListarTerminalesQuery(UUID tenantId, UUID establecimientoId, String search, int page, int size) {
}
