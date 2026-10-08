package com.softprimesolutions.compras.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record OrdenCompraRequest(
        @NotNull UUID proveedorId,
        @NotNull UUID establecimientoDestinoId,
        LocalDate fechaEntregaEstimada,
        @Size(max = 3) String moneda,
        BigDecimal tipoCambio,
        @Size(max = 80) String condicionPago,
        @PositiveOrZero Integer diasCredito,
        @Size(max = 1500) String observacion,
        @NotEmpty @Size(max = 200) List<@Valid @NotNull LineaOrdenCompraRequest> lineas) {
}
