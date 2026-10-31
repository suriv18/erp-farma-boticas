package com.softprimesolutions.inventario.application.dto.result;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PosicionResult(
        UUID id,
        UUID establecimientoId,
        UUID almacenId,
        UUID skuId,
        UUID loteId,
        String numeroLote,
        LocalDate fechaVencimiento,
        String estadoLote,
        String estadoInventario,
        BigDecimal cantidadFisica,
        BigDecimal cantidadReservada,
        BigDecimal cantidadDisponible,
        boolean vendible,
        long version,
        Instant ultimoMovimientoAt) {
}
