package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;

import java.time.Instant;
import java.util.UUID;

public record TerminalProjection(
        UUID uuidPublico,
        UUID tenantUuid,
        UUID establecimientoUuid,
        String codigo,
        String nombre,
        String serieBoletaDefecto,
        String serieFacturaDefecto,
        String numeroSerieEquipo,
        String hostname,
        String ipEquipo,
        String impresoraCodigo,
        boolean storeEdgeHabilitado,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
