package com.softprimesolutions.catalogo.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record PrincipioActivoAsociadoResponse(
        UUID principioActivoId, String concentracionTexto, BigDecimal cantidad, String unidadMedidaCodigo,
        boolean esPrincipal, short orden) {
}
