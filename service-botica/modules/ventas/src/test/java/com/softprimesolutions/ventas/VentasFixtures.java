package com.softprimesolutions.ventas;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.result.LineaVentaResult;
import com.softprimesolutions.ventas.application.dto.result.LoteConsumidoResult;
import com.softprimesolutions.ventas.application.dto.result.PagoResult;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort.SkuVentaRef;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.LineaVenta;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.model.Venta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public final class VentasFixtures {

    public static final UUID TENANT = UUID.fromString("0f6d4c2e-3b1a-4c8e-9a51-7d2b6e4f1a90");
    public static final UUID TERMINAL = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID ESTABLECIMIENTO = UUID.fromString("44444444-4444-4444-8444-444444444444");
    public static final UUID TURNO = UUID.fromString("66666666-6666-4666-8666-666666666666");
    public static final UUID ACTOR_ID = UUID.fromString("77777777-7777-4777-8777-777777777777");
    public static final UUID ALMACEN = UUID.fromString("33333333-3333-4333-8333-333333333333");
    public static final UUID SKU = UUID.fromString("88888888-8888-4888-8888-888888888888");
    public static final UUID OTRO_SKU = UUID.fromString("99999999-9999-4999-8999-999999999999");
    public static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    public static final UUID LINEA = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    public static final UUID LOTE = UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc");
    public static final Actor ACTOR = new Actor(ACTOR_ID);
    public static final Instant AHORA = Instant.parse("2026-06-15T12:00:00Z");
    public static final ApplicationError CONFLICTO =
            new StandardApplicationError("TEST_CONFLICT", "Conflicto de prueba.", ErrorCategory.CONFLICT);

    public static final TransaccionPort TRANSACCION_DIRECTA = new TransaccionPort() {
        @Override
        public <T> Result<T, ApplicationError> ejecutar(Supplier<Result<T, ApplicationError>> trabajo) {
            return trabajo.get();
        }
    };

    private VentasFixtures() {
    }

    public static BigDecimal dec(String valor) {
        return new BigDecimal(valor);
    }

    public static TurnoCaja turno(EstadoTurno estado, String fondo) {
        return TurnoCaja.restore(
                TURNO, TENANT, TERMINAL, ESTABLECIMIENTO, ACTOR, AHORA, dec(fondo), estado, null, dec("0.00"),
                dec(fondo), null, null, null);
    }

    public static TurnoResult turnoResult() {
        return new TurnoResult(
                TURNO, TERMINAL, ESTABLECIMIENTO, ACTOR_ID, AHORA, dec("50.00"), "ABIERTO", null, dec("0.00"),
                dec("50.00"), null, null, null);
    }

    public static SkuVentaRef skuRef(UUID id, boolean operable) {
        return new SkuVentaRef(id, "Paracetamol 500 mg", "UND", false, operable);
    }

    public static Venta venta() {
        var linea = LineaVenta.nueva(LINEA, 1, SKU, "Paracetamol 500 mg", "UND", false, dec("5"), dec("2.50"))
                .fold(value -> value, error -> { throw new AssertionError(error); });
        return Venta.registrar(
                        VENTA, TENANT, TERMINAL, TURNO, ESTABLECIMIENTO, ACTOR, "EST001-POS01-000001", AHORA,
                        List.of(linea), dec("20"))
                .fold(value -> value, error -> { throw new AssertionError(error); });
    }

    public static VentaResult ventaResult() {
        var lote = new LoteConsumidoResult(LOTE, dec("5"));
        var linea = new LineaVentaResult(
                1, SKU, "Paracetamol 500 mg", "UND", dec("5"), dec("2.50"), dec("12.50"), List.of(lote));
        return new VentaResult(
                VENTA, "EST001-POS01-000001", TERMINAL, TURNO, ESTABLECIMIENTO, ACTOR_ID, AHORA, "PEN", dec("12.50"),
                dec("0.00"), dec("0.00"), dec("12.50"), "CONFIRMADA", List.of(linea),
                new PagoResult("EFECTIVO", dec("12.50"), dec("20.00"), dec("7.50")));
    }

    public static <T> Result<T, ApplicationError> ok(T valor) {
        return Result.success(valor);
    }

    public static <T> Result<T, ApplicationError> conflict() {
        return Result.failure(CONFLICTO);
    }
}
