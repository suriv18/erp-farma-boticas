package com.softprimesolutions.compras.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ItemRecepcionRequest(
        @Positive int numeroLineaOrden,
        @NotBlank @Size(max = 120) String numeroLote,
        LocalDate fechaFabricacion,
        @NotNull LocalDate fechaVencimiento,
        @NotNull @Positive BigDecimal cantidadRecibida,
        @PositiveOrZero BigDecimal cantidadRechazada,
        @Size(max = 1000) String motivoRechazo,
        @PositiveOrZero BigDecimal costoUnitario,
        @Size(max = 500) String observacion) {
}
