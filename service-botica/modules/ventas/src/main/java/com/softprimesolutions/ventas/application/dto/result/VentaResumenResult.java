package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VentaResumenResult(
        UUID id, String numeroOperacion, UUID terminalId, Instant fechaVenta, BigDecimal total, String estado) {
}
