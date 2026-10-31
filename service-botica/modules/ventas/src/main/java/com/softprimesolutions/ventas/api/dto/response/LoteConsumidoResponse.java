package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record LoteConsumidoResponse(UUID loteId, BigDecimal cantidad) {
}
