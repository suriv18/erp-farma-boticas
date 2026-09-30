package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearTerminalPosRequest(
        @NotNull UUID tenantId,
        @NotNull UUID establecimientoId,
        @NotBlank @Size(min = 1, max = 40) String codigo,
        @NotBlank @Size(min = 2, max = 120) String nombre,
        @Size(max = 4) String serieBoletaDefecto,
        @Size(max = 4) String serieFacturaDefecto,
        @Size(max = 120) String numeroSerieEquipo,
        @Size(max = 150) String hostname,
        String ipEquipo,
        @Size(max = 100) String impresoraCodigo,
        boolean storeEdgeHabilitado) {
}
