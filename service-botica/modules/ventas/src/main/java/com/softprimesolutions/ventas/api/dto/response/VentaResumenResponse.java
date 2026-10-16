package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record VentaResumenResponse(
        UUID id, String numeroOperacion, UUID terminalId, Instant fechaVenta, BigDecimal total, String estado) {
}
