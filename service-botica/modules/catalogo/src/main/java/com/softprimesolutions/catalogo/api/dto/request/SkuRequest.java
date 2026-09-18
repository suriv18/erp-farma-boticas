package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.util.UUID;

public record SkuRequest(
        @NotNull UUID tenantId,
        UUID productoReguladoId,
        UUID categoriaId,
        UUID marcaId,
        @NotBlank String tipoSku,
        @NotBlank @Size(min = 2, max = 60) String codigoInterno,
        @NotBlank @Size(min = 2, max = 500) String descripcionComercial,
        @Size(max = 200) String nombreCorto,
        @Size(max = 300) String presentacionComercial,
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
        String imagenUri) {
}
