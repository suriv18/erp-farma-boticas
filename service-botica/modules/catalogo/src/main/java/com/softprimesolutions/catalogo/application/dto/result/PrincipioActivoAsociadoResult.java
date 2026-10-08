package com.softprimesolutions.catalogo.application.dto.result;

import java.math.BigDecimal;
import java.util.UUID;

public record PrincipioActivoAsociadoResult(
        UUID principioActivoId, String concentracionTexto, BigDecimal cantidad, String unidadMedidaCodigo,
        boolean esPrincipal, short orden) {
}
