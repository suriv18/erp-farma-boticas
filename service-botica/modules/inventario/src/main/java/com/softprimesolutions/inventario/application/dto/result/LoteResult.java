package com.softprimesolutions.inventario.application.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoteResult(
        UUID id,
        UUID skuId,
        String numeroLote,
        LocalDate fechaVencimiento,
        String estado,
        String motivoEstado,
        Instant bloqueadoAt,
        boolean vendible) {
}
