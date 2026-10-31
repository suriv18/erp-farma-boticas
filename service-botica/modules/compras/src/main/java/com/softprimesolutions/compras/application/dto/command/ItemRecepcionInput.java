package com.softprimesolutions.compras.application.dto.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemRecepcionInput(
        int numeroLineaOrden,
        String numeroLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        BigDecimal cantidadRecibida,
        BigDecimal cantidadRechazada,
        String motivoRechazo,
        BigDecimal costoUnitario,
        String observacion) {
}
