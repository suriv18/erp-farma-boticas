package com.softprimesolutions.organizacion.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearAlmacenCommand(
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
        BigDecimal temperaturaMaxC) {
}
