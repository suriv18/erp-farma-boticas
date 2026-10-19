package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearAlmacenRequest(
        @NotNull UUID establecimientoId,
        @NotBlank @Size(min = 1, max = 40) String codigo,
        @NotBlank @Size(min = 2, max = 150) String nombre,
        @NotBlank String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC) {
}
