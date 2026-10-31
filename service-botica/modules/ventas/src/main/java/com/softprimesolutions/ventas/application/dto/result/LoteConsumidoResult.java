package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

public record LoteConsumidoResult(UUID loteId, BigDecimal cantidad) {
}
