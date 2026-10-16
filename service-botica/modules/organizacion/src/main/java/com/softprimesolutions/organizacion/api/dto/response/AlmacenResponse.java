package com.softprimesolutions.organizacion.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlmacenResponse(
        UUID id,
        UUID tenantId,
        UUID establecimientoId,
        String codigo,
        String nombre,
        String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC,
        boolean activo,
        Instant createdAt,
        Instant updatedAt) {
}
