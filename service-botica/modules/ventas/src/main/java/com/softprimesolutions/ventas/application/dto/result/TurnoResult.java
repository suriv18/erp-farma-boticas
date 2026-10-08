package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record TurnoResult(
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
