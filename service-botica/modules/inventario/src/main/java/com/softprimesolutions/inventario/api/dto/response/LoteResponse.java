package com.softprimesolutions.inventario.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record LoteResponse(
        UUID id,
        UUID skuId,
        String numeroLote,
        LocalDate fechaVencimiento,
        String estado,
        String motivoEstado,
        Instant bloqueadoAt,
        boolean vendible) {
}
