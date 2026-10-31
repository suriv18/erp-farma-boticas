package com.softprimesolutions.inventario.api;

import java.math.BigDecimal;
import java.util.UUID;

public record MovimientoReintegrado(UUID movimientoId, UUID loteId, BigDecimal cantidad, BigDecimal stockPosterior) {
}
