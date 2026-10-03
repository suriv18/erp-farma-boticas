package com.softprimesolutions.ventas;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

public final class VentasFixtures {

    public static final UUID TENANT = UUID.fromString("0f6d4c2e-3b1a-4c8e-9a51-7d2b6e4f1a90");
    public static final UUID TERMINAL = UUID.fromString("11111111-1111-4111-8111-111111111111");
    public static final UUID ESTABLECIMIENTO = UUID.fromString("44444444-4444-4444-8444-444444444444");
    public static final UUID TURNO = UUID.fromString("66666666-6666-4666-8666-666666666666");
    public static final UUID ACTOR_ID = UUID.fromString("77777777-7777-4777-8777-777777777777");
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

    public static <T> Result<T, ApplicationError> ok(T valor) {
        return Result.success(valor);
    }

    public static <T> Result<T, ApplicationError> conflict() {
        return Result.failure(CONFLICTO);
    }
}
