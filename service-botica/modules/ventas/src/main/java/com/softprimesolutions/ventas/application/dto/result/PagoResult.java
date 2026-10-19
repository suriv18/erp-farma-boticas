package com.softprimesolutions.ventas.application.dto.result;

import java.math.BigDecimal;

public record PagoResult(String medioPago, BigDecimal monto, BigDecimal montoRecibido, BigDecimal vuelto) {
}
