package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record CrearTerminalPosCommand(
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
        boolean storeEdgeHabilitado) {
}
