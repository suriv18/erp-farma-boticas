package com.softprimesolutions.ventas.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record LineaVentaInput(UUID skuId, BigDecimal cantidad, BigDecimal precioUnitario) {
}
