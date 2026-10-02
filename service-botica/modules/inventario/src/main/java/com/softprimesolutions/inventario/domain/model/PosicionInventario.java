package com.softprimesolutions.inventario.domain.model;

import com.softprimesolutions.inventario.domain.exception.InventarioErrorCodes;
import com.softprimesolutions.inventario.domain.valueobject.PosicionId;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public final class PosicionInventario {

    private static final BigDecimal CANTIDAD_MAXIMA = new BigDecimal("1000000000");
    private static final int ESCALA_MAXIMA = 4;

    private final PosicionId id;
    private final UUID loteId;
    private final UUID almacenId;
    private final UUID skuId;
    private final BigDecimal cantidadFisica;
    private final BigDecimal cantidadReservada;
    private final long version;

    private PosicionInventario(
            PosicionId id, UUID loteId, UUID almacenId, UUID skuId, BigDecimal cantidadFisica,
            BigDecimal cantidadReservada, long version) {
        this.id = Objects.requireNonNull(id, "id es obligatorio");
        this.loteId = Objects.requireNonNull(loteId, "loteId es obligatorio");
        this.almacenId = Objects.requireNonNull(almacenId, "almacenId es obligatorio");
        this.skuId = Objects.requireNonNull(skuId, "skuId es obligatorio");
        this.cantidadFisica = Objects.requireNonNull(cantidadFisica, "cantidadFisica es obligatoria");
        this.cantidadReservada = Objects.requireNonNull(cantidadReservada, "cantidadReservada es obligatoria");
        this.version = version;
    }

    public static PosicionInventario nueva(PosicionId id, UUID loteId, UUID almacenId, UUID skuId) {
        return new PosicionInventario(id, loteId, almacenId, skuId, BigDecimal.ZERO, BigDecimal.ZERO, 0L);
    }

    public static PosicionInventario restore(
            PosicionId id, UUID loteId, UUID almacenId, UUID skuId, BigDecimal cantidadFisica,
            BigDecimal cantidadReservada, long version) {
        return new PosicionInventario(id, loteId, almacenId, skuId, cantidadFisica, cantidadReservada, version);
    }

    public Result<AplicacionMovimiento, ErrorDetail> aplicar(TipoMovimiento tipo, BigDecimal cantidad) {
        Objects.requireNonNull(tipo, "tipo es obligatorio");
        if (cantidad == null || cantidad.signum() <= 0 || cantidad.compareTo(CANTIDAD_MAXIMA) > 0) {
            return failure(InventarioErrorCodes.CANTIDAD_INVALIDA,
                    "La cantidad debe ser mayor que cero y no superar 1000000000.");
        }
        if (cantidad.stripTrailingZeros().scale() > ESCALA_MAXIMA) {
            return failure(InventarioErrorCodes.CANTIDAD_INVALIDA, "La cantidad admite hasta 4 decimales.");
        }
        var posterior = tipo.ingreso() ? cantidadFisica.add(cantidad) : cantidadFisica.subtract(cantidad);
        if (posterior.compareTo(cantidadReservada) < 0) {
            return failure(InventarioErrorCodes.STOCK_INSUFICIENTE,
                    "El stock disponible no alcanza para la salida solicitada.");
        }
        return Result.success(new AplicacionMovimiento(
                new PosicionInventario(id, loteId, almacenId, skuId, posterior, cantidadReservada, version),
                cantidadFisica, posterior));
    }

    private static <T> Result<T, ErrorDetail> failure(String code, String message) {
        return Result.failure(new ErrorDetail(code, message, Map.of()));
    }

    public PosicionId id() { return id; }
    public UUID loteId() { return loteId; }
    public UUID almacenId() { return almacenId; }
    public UUID skuId() { return skuId; }
    public BigDecimal cantidadFisica() { return cantidadFisica; }
    public BigDecimal cantidadReservada() { return cantidadReservada; }
    public long version() { return version; }
}
