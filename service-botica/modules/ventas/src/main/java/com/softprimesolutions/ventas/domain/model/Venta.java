package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.domain.model.Failures.failure;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public final class Venta {

    private static final int MAXIMO_LINEAS = 100;

    private final UUID id;
    private final UUID tenantId;
    private final UUID terminalId;
    private final UUID turnoId;
    private final UUID establecimientoId;
    private final Actor vendedor;
    private final String numeroOperacion;
    private final Instant fechaVenta;
    private final List<LineaVenta> lineas;
    private final BigDecimal subtotal;
    private final PagoEfectivo pago;

    private Venta(
            UUID id, UUID tenantId, UUID terminalId, UUID turnoId, UUID establecimientoId, Actor vendedor,
            String numeroOperacion, Instant fechaVenta, List<LineaVenta> lineas, BigDecimal subtotal,
            PagoEfectivo pago) {
        this.id = id;
        this.tenantId = tenantId;
        this.terminalId = terminalId;
        this.turnoId = turnoId;
        this.establecimientoId = establecimientoId;
        this.vendedor = vendedor;
        this.numeroOperacion = numeroOperacion;
        this.fechaVenta = fechaVenta;
        this.lineas = List.copyOf(lineas);
        this.subtotal = subtotal;
        this.pago = pago;
    }

    public static Result<Venta, ErrorDetail> registrar(
            UUID id, UUID tenantId, UUID terminalId, UUID turnoId, UUID establecimientoId, Actor vendedor,
            String numeroOperacion, Instant fechaVenta, List<LineaVenta> lineas, BigDecimal montoRecibido) {
        if (lineas.isEmpty()) {
            return failure(VentasErrorCodes.VENTA_SIN_LINEAS, "La venta debe tener al menos una linea.");
        }
        if (lineas.size() > MAXIMO_LINEAS) {
            return failure(VentasErrorCodes.VENTA_LINEAS_EXCEDIDAS, "La venta admite hasta 100 lineas.");
        }
        var subtotal = Importes.redondear(
                lineas.stream().map(LineaVenta::totalLinea).reduce(BigDecimal.ZERO, BigDecimal::add));
        if (subtotal.signum() <= 0) {
            return failure(VentasErrorCodes.TOTAL_INVALIDO, "El total de la venta debe ser mayor que cero.");
        }
        return PagoEfectivo.cobrar(subtotal, montoRecibido).map(pago -> new Venta(
                id, tenantId, terminalId, turnoId, establecimientoId, vendedor, numeroOperacion, fechaVenta, lineas,
                subtotal, pago));
    }

    public UUID id() { return id; }
    public UUID tenantId() { return tenantId; }
    public UUID terminalId() { return terminalId; }
    public UUID turnoId() { return turnoId; }
    public UUID establecimientoId() { return establecimientoId; }
    public Actor vendedor() { return vendedor; }
    public String numeroOperacion() { return numeroOperacion; }
    public Instant fechaVenta() { return fechaVenta; }
    public List<LineaVenta> lineas() { return lineas; }
    public BigDecimal subtotal() { return subtotal; }
    public BigDecimal total() { return subtotal; }
    public PagoEfectivo pago() { return pago; }
}
