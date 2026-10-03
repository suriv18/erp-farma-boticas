package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;
import java.util.UUID;

public final class LineaVenta {

    private final UUID id;
    private final int numeroLinea;
    private final UUID skuId;
    private final String descripcion;
    private final String unidadVentaCodigo;
    private final boolean esFraccion;
    private final BigDecimal cantidad;
    private final BigDecimal precioUnitario;
    private final BigDecimal totalLinea;

    private LineaVenta(
            UUID id, int numeroLinea, UUID skuId, String descripcion, String unidadVentaCodigo, boolean esFraccion,
            BigDecimal cantidad, BigDecimal precioUnitario, BigDecimal totalLinea) {
        this.id = id;
        this.numeroLinea = numeroLinea;
        this.skuId = skuId;
        this.descripcion = descripcion;
        this.unidadVentaCodigo = unidadVentaCodigo;
        this.esFraccion = esFraccion;
        this.cantidad = cantidad;
        this.precioUnitario = precioUnitario;
        this.totalLinea = totalLinea;
    }

    public static Result<LineaVenta, ErrorDetail> nueva(
            UUID id, int numeroLinea, UUID skuId, String descripcion, String unidadVentaCodigo,
            boolean permiteFraccion, BigDecimal cantidad, BigDecimal precioUnitario) {
        if (!Importes.cantidadValida(cantidad)) {
            return failure(VentasErrorCodes.CANTIDAD_INVALIDA,
                    "La cantidad debe ser mayor que cero, no superar 1000000000 y tener hasta 4 decimales.");
        }
        if (!Importes.precioValido(precioUnitario)) {
            return failure(VentasErrorCodes.PRECIO_INVALIDO,
                    "El precio unitario debe estar entre 0 y 1000000000 con hasta 4 decimales.");
        }
        var esFraccion = cantidad.stripTrailingZeros().scale() > 0;
        if (esFraccion && !permiteFraccion) {
            return failure(VentasErrorCodes.FRACCION_NO_PERMITIDA, "El producto no se vende por fraccion.");
        }
        return Result.success(new LineaVenta(
                id, numeroLinea, skuId, descripcion, unidadVentaCodigo, esFraccion, cantidad, precioUnitario,
                Importes.redondear(cantidad.multiply(precioUnitario))));
    }

    public UUID id() { return id; }
    public int numeroLinea() { return numeroLinea; }
    public UUID skuId() { return skuId; }
    public String descripcion() { return descripcion; }
    public String unidadVentaCodigo() { return unidadVentaCodigo; }
    public boolean esFraccion() { return esFraccion; }
    public BigDecimal cantidad() { return cantidad; }
    public BigDecimal precioUnitario() { return precioUnitario; }
    public BigDecimal totalLinea() { return totalLinea; }
}
