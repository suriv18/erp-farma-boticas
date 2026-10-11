package com.softprimesolutions.ventas.domain.model;

import java.math.BigDecimal;
import java.util.UUID;

public record LoteConsumo(UUID loteId, BigDecimal cantidad) {
}
