package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarTerminalPosRequest(
        @NotBlank @Size(min = 2, max = 120) String nombre,
        @Size(max = 4) String serieBoletaDefecto,
        @Size(max = 4) String serieFacturaDefecto,
        @Size(max = 120) String numeroSerieEquipo,
        @Size(max = 150) String hostname,
        String ipEquipo,
        @Size(max = 100) String impresoraCodigo,
        boolean storeEdgeHabilitado,
        @NotBlank String estado) {
}
