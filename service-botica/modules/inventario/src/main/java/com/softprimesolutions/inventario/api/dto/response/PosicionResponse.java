package com.softprimesolutions.inventario.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record PosicionResponse(
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
