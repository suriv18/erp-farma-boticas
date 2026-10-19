package com.softprimesolutions.compras.domain.model;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public record LineaNueva(
        UUID skuId,
        String descripcionSnapshot,
        BigDecimal cantidad,
        String unidadMedidaCodigo,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal toleranciaExcesoPct,
        BigDecimal toleranciaDefectoPct) {

    public LineaNueva {
        Objects.requireNonNull(skuId, "skuId es obligatorio");
        Objects.requireNonNull(cantidad, "cantidad es obligatoria");
        Objects.requireNonNull(unidadMedidaCodigo, "unidadMedidaCodigo es obligatorio");
        Objects.requireNonNull(precioUnitario, "precioUnitario es obligatorio");
        Objects.requireNonNull(descuento, "descuento es obligatorio");
        Objects.requireNonNull(impuesto, "impuesto es obligatorio");
        Objects.requireNonNull(toleranciaExcesoPct, "toleranciaExcesoPct es obligatoria");
        Objects.requireNonNull(toleranciaDefectoPct, "toleranciaDefectoPct es obligatoria");
    }
}
