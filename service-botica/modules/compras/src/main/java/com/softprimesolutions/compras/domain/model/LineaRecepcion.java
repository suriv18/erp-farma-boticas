package com.softprimesolutions.compras.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LineaRecepcion(
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
        String observacion) {

    public boolean ingresaStock() {
        return cantidadAceptada.signum() > 0;
    }
}
