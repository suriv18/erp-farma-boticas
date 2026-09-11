package com.softprimesolutions.catalogo.application.dto.command;

import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.math.BigDecimal;
import java.util.UUID;

public record CrearSkuCommand(
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
        boolean requiereLote,
        boolean requiereVencimiento,
        boolean afectoIgv,
        BigDecimal stockMinimoDefault,
        BigDecimal stockMaximoDefault,
        String imagenUri,
        String createdBy) implements Command<SkuResult> {
}
