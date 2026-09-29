package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record ActualizarTerminalPosCommand(
        UUID terminalId,
        UUID tenantId,
        String nombre,
        String serieBoletaDefecto,
        String serieFacturaDefecto,
        String numeroSerieEquipo,
        String hostname,
        String ipEquipo,
        String impresoraCodigo,
        boolean storeEdgeHabilitado,
        String estado) {
}
