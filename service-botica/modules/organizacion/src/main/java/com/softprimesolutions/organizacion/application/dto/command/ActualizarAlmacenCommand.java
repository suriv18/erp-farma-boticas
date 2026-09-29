package com.softprimesolutions.organizacion.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record ActualizarAlmacenCommand(
        UUID almacenId,
        UUID tenantId,
        String nombre,
        String tipo,
        boolean permiteLotes,
        boolean permiteVencimiento,
        boolean permiteVenta,
        boolean permiteDespacho,
        boolean controlTemperatura,
        BigDecimal temperaturaMinC,
        BigDecimal temperaturaMaxC,
        boolean activo) {
}
