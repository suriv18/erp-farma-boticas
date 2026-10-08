package com.softprimesolutions.inventario.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record MovimientoResponse(
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
