package com.softprimesolutions.ventas.api.dto.response;

import java.math.BigDecimal;

public record PagoResponse(String medioPago, BigDecimal monto, BigDecimal montoRecibido, BigDecimal vuelto) {
}
