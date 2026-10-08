package com.softprimesolutions.compras.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CondicionesOrden(
        LocalDate fechaEntregaEstimada,
        String moneda,
        BigDecimal tipoCambio,
        String condicionPago,
        int diasCredito,
        String observacion) {
}
