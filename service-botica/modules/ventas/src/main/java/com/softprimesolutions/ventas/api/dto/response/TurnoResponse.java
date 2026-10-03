package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TurnoResponse(
        UUID id,
        UUID terminalId,
        UUID establecimientoId,
        UUID cajeroId,
        Instant aperturaAt,
        BigDecimal fondoInicial,
        String estado,
        Instant cierreAt,
        BigDecimal totalVentasSistema,
        BigDecimal totalSistema,
        BigDecimal totalDeclarado,
        BigDecimal diferencia,
        String observacionCierre) {
}
