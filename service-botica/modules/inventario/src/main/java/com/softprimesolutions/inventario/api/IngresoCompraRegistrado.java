package com.softprimesolutions.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

public record IngresoCompraRegistrado(
        UUID movimientoId,
        UUID loteId,
        UUID posicionId,
        BigDecimal stockAnterior,
        BigDecimal stockPosterior) {
}
