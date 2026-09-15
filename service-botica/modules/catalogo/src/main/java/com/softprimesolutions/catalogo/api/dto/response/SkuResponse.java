package com.softprimesolutions.catalogo.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SkuResponse(
        UUID id,
        UUID tenantId,
        UUID productoReguladoId,
        UUID categoriaId,
        UUID marcaId,
        String tipoSku,
        String codigoInterno,
        String descripcionComercial,
        String nombreCorto,
        String presentacionComercial,
        String unidadVentaCodigo,
        BigDecimal contenido,
        String unidadContenidoCodigo,
        BigDecimal pesoGramos,
        BigDecimal altoCm,
        BigDecimal anchoCm,
        BigDecimal largoCm,
        boolean permiteVentaFraccion,
        BigDecimal factorFraccion,
        String unidadFraccionCodigo,
        boolean requiereLote,
        boolean requiereVencimiento,
        boolean afectoIgv,
        BigDecimal stockMinimoDefault,
        BigDecimal stockMaximoDefault,
        String imagenUri,
        List<CodigoBarraSkuResponse> codigosBarra,
        String estado,
        String createdBy,
        Instant createdAt,
        String updatedBy,
        Instant updatedAt) {
}
