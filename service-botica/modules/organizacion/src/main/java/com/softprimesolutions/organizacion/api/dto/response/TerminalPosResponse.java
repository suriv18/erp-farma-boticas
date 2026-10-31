package com.softprimesolutions.organizacion.api.dto.response;

import java.time.Instant;
import java.util.UUID;

public record TerminalPosResponse(
        UUID id,
        UUID tenantId,
        UUID establecimientoId,
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
