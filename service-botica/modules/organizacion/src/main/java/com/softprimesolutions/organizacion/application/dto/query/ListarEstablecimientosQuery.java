package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ListarEstablecimientosQuery(UUID tenantId, UUID empresaId, String search, int page, int size) {
}
