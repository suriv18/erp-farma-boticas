package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record AlmacenProjection(
        UUID uuidPublico,
        UUID tenantUuid,
        UUID establecimientoUuid,
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
