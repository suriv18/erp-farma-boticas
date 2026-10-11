package com.softprimesolutions.catalogo.api.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record SkuResumenResponse(
        UUID id,
        String codigoInterno,
        String descripcionComercial,
        String tipoSku,
        String estado,
        String unidadVentaCodigo,
        boolean permiteVentaFraccion,
        BigDecimal precioVentaReferencia) {
}
