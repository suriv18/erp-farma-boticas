package com.softprimesolutions.ventas.application.dto.query;

import java.time.Instant;
import java.util.UUID;

public record ListarVentasQuery(
        UUID tenantId, UUID establecimientoId, Instant desde, Instant hasta, int page, int size) {
}
