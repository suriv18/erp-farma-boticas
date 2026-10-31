package com.softprimesolutions.inventario;

import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.dto.result.PaginaResult;
import com.softprimesolutions.inventario.application.dto.result.PosicionResult;
import com.softprimesolutions.inventario.domain.model.EstadoLote;
import com.softprimesolutions.inventario.domain.model.Lote;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import com.softprimesolutions.inventario.domain.valueobject.Actor;
import com.softprimesolutions.inventario.domain.valueobject.LoteId;
import com.softprimesolutions.inventario.domain.valueobject.PosicionId;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public final class InventarioFixtures {

    public static final UUID TENANT = UUID.fromString("0f6d4c2e-3b1a-4c8e-9a51-7d2b6e4f1a90");
    public static final UUID SKU = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID OTRO_SKU = UUID.fromString("22222222-2222-4222-8222-222222222222");
    public static final UUID ALMACEN = UUID.fromString("33333333-3333-4333-8333-333333333333");
    public static final UUID ESTABLECIMIENTO = UUID.fromString("44444444-4444-4444-8444-444444444444");
    public static final UUID LOTE = UUID.fromString("55555555-5555-4555-8555-555555555555");
    public static final UUID POSICION = UUID.fromString("66666666-6666-4666-8666-666666666666");
    public static final UUID ACTOR_ID = UUID.fromString("77777777-7777-4777-8777-777777777777");
    public static final Actor ACTOR = new Actor(ACTOR_ID);
    public static final Instant AHORA = Instant.parse("2026-06-15T12:00:00Z");
    public static final LocalDate HOY = LocalDate.of(2026, 6, 15);
    public static final LocalDate VENCIMIENTO = LocalDate.of(2027, 6, 30);
    public static final ApplicationError CONFLICTO =
            new StandardApplicationError("TEST_CONFLICT", "Conflicto de prueba.", ErrorCategory.CONFLICT);

    private InventarioFixtures() {
    }

    public static Lote lote(EstadoLote estado, LocalDate vencimiento) {
        return Lote.restore(
                new LoteId(LOTE), new TenantId(TENANT), SKU, "L-001", vencimiento, estado,
                estado == EstadoLote.BLOQUEADO ? "motivo previo" : null, null, null, AHORA, null, null);
    }

    public static Lote loteHabilitado() {
        return lote(EstadoLote.HABILITADO, VENCIMIENTO);
    }

    public static PosicionInventario posicion(String fisica, String reservada, long version) {
        return PosicionInventario.restore(
                new PosicionId(POSICION), LOTE, ALMACEN, SKU, new BigDecimal(fisica), new BigDecimal(reservada),
                version);
    }

    public static LoteResult loteResult() {
        return new LoteResult(LOTE, SKU, "L-001", VENCIMIENTO, "HABILITADO", null, null, true);
    }

    public static PosicionResult posicionResult() {
        return new PosicionResult(
                POSICION, ESTABLECIMIENTO, ALMACEN, SKU, LOTE, "L-001", VENCIMIENTO, "HABILITADO", "DISPONIBLE",
                new BigDecimal("10.0000"), new BigDecimal("2.0000"), new BigDecimal("8.0000"), true, 3L, AHORA);
    }

    public static PaginaResult<PosicionResult> paginaPosiciones() {
        return new PaginaResult<>(List.of(posicionResult()), 0, 20, 1);
    }

    public static <T> Result<T, ApplicationError> ok(T value) {
        return Result.success(value);
    }

    public static <T> Result<T, ApplicationError> conflict() {
        return Result.failure(CONFLICTO);
    }
}
