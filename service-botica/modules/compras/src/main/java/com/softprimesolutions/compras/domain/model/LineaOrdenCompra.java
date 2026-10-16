package com.softprimesolutions.compras.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

public record LineaOrdenCompra(
        int numeroLinea,
        UUID skuId,
        String descripcionSnapshot,
        BigDecimal cantidad,
        String unidadMedidaCodigo,
        BigDecimal precioUnitario,
        BigDecimal descuento,
        BigDecimal impuesto,
        BigDecimal totalLinea,
        BigDecimal toleranciaExcesoPct,
        BigDecimal toleranciaDefectoPct,
        BigDecimal cantidadRecibida) {

    private static final int ESCALA_CANTIDAD = 4;
    private static final int ESCALA_IMPORTE = 2;

    public static BigDecimal importeBruto(BigDecimal cantidad, BigDecimal precioUnitario) {
        return cantidad.multiply(precioUnitario).setScale(ESCALA_IMPORTE, RoundingMode.HALF_UP);
    }

    public BigDecimal importeBruto() {
        return importeBruto(cantidad, precioUnitario);
    }

    public BigDecimal pendiente() {
        return cantidad.subtract(cantidadRecibida).max(BigDecimal.ZERO);
    }

    public BigDecimal maximoRecibible() {
        return cantidad.multiply(BigDecimal.ONE.add(toleranciaExcesoPct.movePointLeft(2)))
                .subtract(cantidadRecibida)
                .setScale(ESCALA_CANTIDAD, RoundingMode.DOWN)
                .max(BigDecimal.ZERO);
    }

    public boolean completa() {
        var minimo = cantidad.multiply(BigDecimal.ONE.subtract(toleranciaDefectoPct.movePointLeft(2)));
        return cantidadRecibida.compareTo(minimo) >= 0;
    }

    public LineaOrdenCompra conRecibido(BigDecimal aceptada) {
        return new LineaOrdenCompra(
                numeroLinea, skuId, descripcionSnapshot, cantidad, unidadMedidaCodigo, precioUnitario, descuento,
                impuesto, totalLinea, toleranciaExcesoPct, toleranciaDefectoPct, cantidadRecibida.add(aceptada));
    }
}
