package com.softprimesolutions.compras.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

public record ItemRecepcion(
        int numeroLineaOrden,
        String numeroLote,
        LocalDate fechaFabricacion,
        LocalDate fechaVencimiento,
        BigDecimal cantidadRecibida,
        BigDecimal cantidadRechazada,
        String motivoRechazo,
        BigDecimal costoUnitario,
        String observacion) {

    public ItemRecepcion {
        Objects.requireNonNull(cantidadRecibida, "cantidadRecibida es obligatoria");
        Objects.requireNonNull(cantidadRechazada, "cantidadRechazada es obligatoria");
    }
}
