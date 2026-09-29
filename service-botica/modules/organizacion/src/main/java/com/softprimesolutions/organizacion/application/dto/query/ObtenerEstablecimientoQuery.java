package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerEstablecimientoQuery(UUID tenantId, UUID establecimientoId) {
}
