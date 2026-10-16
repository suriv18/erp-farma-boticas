package com.softprimesolutions.compras.api.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record RecepcionRequest(
        @NotNull UUID ordenCompraId,
        @NotNull UUID almacenId,
        @Size(max = 2) String documentoProveedorTipo,
        @Size(max = 20) String documentoProveedorSerie,
        @Size(max = 40) String documentoProveedorNumero,
        @Size(max = 80) String guiaRemisionRemitente,
        @Size(max = 80) String guiaRemisionTransportista,
        BigDecimal temperaturaRecepcionC,
        BigDecimal humedadRelativaPct,
        @Size(max = 1000) String observacion,
        @NotEmpty @Size(max = 200) List<@Valid @NotNull ItemRecepcionRequest> items) {
}
