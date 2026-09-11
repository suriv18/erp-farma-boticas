package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.util.UUID;

public record AsociarPrincipioActivoRequest(
        @NotNull UUID principioActivoId, String concentracionTexto, BigDecimal cantidad,
        String unidadMedidaCodigo, boolean esPrincipal, short orden) {
}
