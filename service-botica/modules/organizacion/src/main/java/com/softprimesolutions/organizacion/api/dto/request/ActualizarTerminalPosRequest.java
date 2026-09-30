package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarTerminalPosRequest(
        @NotBlank @Size(min = 2, max = 250) String nombre,
        @Size(max = 10) String serieBoletaDefecto,
        @Size(max = 10) String serieFacturaDefecto,
        @Size(max = 100) String numeroSerieEquipo,
        @Size(max = 150) String hostname,
        String ipEquipo,
        @Size(max = 60) String impresoraCodigo,
        boolean storeEdgeHabilitado,
        @NotBlank String estado) {
}
