package com.softprimesolutions.inventario.application.port.out;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record SalidaDeVenta(
        UUID movimientoId,
        UUID almacenId,
        UUID skuId,
        UUID loteId,
        String numeroLote,
        LocalDate fechaVencimiento,
        BigDecimal cantidad) {
}
