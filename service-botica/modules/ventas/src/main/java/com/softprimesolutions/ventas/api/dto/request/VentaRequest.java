package com.softprimesolutions.ventas.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;
import java.util.UUID;

public record VentaRequest(
        @NotNull UUID terminalId,
        @NotNull UUID almacenId,
        @NotEmpty @Size(max = 100) List<@Valid @NotNull LineaVentaRequest> lineas,
        @NotNull @Valid PagoEfectivoRequest pago) {
}
