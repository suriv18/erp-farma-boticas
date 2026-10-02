package com.softprimesolutions.inventario.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovimientoResult(
        UUID id,
        UUID posicionId,
        UUID loteId,
        String tipo,
        String naturaleza,
        BigDecimal cantidad,
        BigDecimal stockAnterior,
        BigDecimal stockPosterior,
        Instant fechaNegocio) {
}
