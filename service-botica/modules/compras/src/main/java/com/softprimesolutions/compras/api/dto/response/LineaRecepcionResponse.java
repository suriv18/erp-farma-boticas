package com.softprimesolutions.compras.api.dto.response;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LineaRecepcionResponse(
        UUID id,
        int numeroLinea,
        int numeroLineaOrden,
        UUID skuId,
        String numeroLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        BigDecimal cantidadRecibida,
        BigDecimal cantidadAceptada,
        BigDecimal cantidadRechazada,
        BigDecimal costoUnitario,
        String decisionCalidad,
        String motivoDecision,
        String observacion,
        UUID loteId) {
}
