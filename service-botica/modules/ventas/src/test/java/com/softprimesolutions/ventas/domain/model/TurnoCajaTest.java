package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class TurnoCajaTest {

    private static final Instant CIERRE = Instant.parse("2026-06-15T20:00:00Z");

    private static TurnoCaja value(Result<TurnoCaja, ErrorDetail> result) {
        return result.fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static String code(Result<TurnoCaja, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    private static Result<TurnoCaja, ErrorDetail> abrir(BigDecimal fondo) {
        return TurnoCaja.abrir(TURNO, TENANT, TERMINAL, ESTABLECIMIENTO, ACTOR, fondo, AHORA);
    }

    @Test
    void opensATurnoWithTheInitialFundAsTheSystemTotal() {
        var turno = value(abrir(dec("50")));

        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(turno.tenantId()).isEqualTo(TENANT);
        assertThat(turno.terminalId()).isEqualTo(TERMINAL);
        assertThat(turno.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(turno.cajero()).isEqualTo(ACTOR);
        assertThat(turno.aperturaAt()).isEqualTo(AHORA);
        assertThat(turno.estado()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(turno.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(turno.totalVentasSistema()).isEqualTo(dec("0.00"));
        assertThat(turno.totalSistema()).isEqualTo(dec("50.00"));
        assertThat(turno.cierreAt()).isNull();
        assertThat(turno.totalDeclarado()).isNull();
        assertThat(turno.diferencia()).isNull();
        assertThat(turno.observacionCierre()).isNull();
    }

    @Test
    void rejectsAnInvalidInitialFund() {
        assertThat(code(abrir(null))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abrir(dec("-1")))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abrir(dec("1.234")))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abrir(dec("1000000001")))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
    }

    @Test
    void closesComputingTheSystemTotalAndTheDifference() {
        var cerrado = value(turno(EstadoTurno.ABIERTO, "100.00")
                .cerrar(dec("40"), dec("135"), "  Faltan 5 soles  ", CIERRE));

        assertThat(cerrado.estado()).isEqualTo(EstadoTurno.CERRADO);
        assertThat(cerrado.cierreAt()).isEqualTo(CIERRE);
        assertThat(cerrado.totalVentasSistema()).isEqualTo(dec("40.00"));
        assertThat(cerrado.totalSistema()).isEqualTo(dec("140.00"));
        assertThat(cerrado.totalDeclarado()).isEqualTo(dec("135.00"));
        assertThat(cerrado.diferencia()).isEqualTo(dec("-5.00"));
        assertThat(cerrado.observacionCierre()).isEqualTo("Faltan 5 soles");
        assertThat(cerrado.id()).isEqualTo(TURNO);
        assertThat(cerrado.fondoInicial()).isEqualTo(dec("100.00"));
    }

    @Test
    void aBlankOrMissingObservationIsStoredAsNull() {
        var sinNota = value(turno(EstadoTurno.ABIERTO, "10").cerrar(dec("0"), dec("10"), null, CIERRE));
        var enBlanco = value(turno(EstadoTurno.ABIERTO, "10").cerrar(dec("0"), dec("10"), "   ", CIERRE));

        assertThat(sinNota.observacionCierre()).isNull();
        assertThat(enBlanco.observacionCierre()).isNull();
        assertThat(sinNota.diferencia()).isEqualTo(dec("0.00"));
    }

    @Test
    void onlyAnOpenTurnoCanBeClosed() {
        assertThat(code(turno(EstadoTurno.CERRADO, "10").cerrar(dec("0"), dec("10"), null, CIERRE)))
                .isEqualTo(VentasErrorCodes.TURNO_ESTADO_INVALIDO);
        assertThat(code(turno(EstadoTurno.EN_ARQUEO, "10").cerrar(dec("0"), dec("10"), null, CIERRE)))
                .isEqualTo(VentasErrorCodes.TURNO_ESTADO_INVALIDO);
    }

    @Test
    void rejectsAnInvalidDeclaredTotalOrATooLongObservation() {
        var abierto = turno(EstadoTurno.ABIERTO, "10");

        assertThat(code(abierto.cerrar(dec("0"), null, null, CIERRE))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abierto.cerrar(dec("0"), dec("-1"), null, CIERRE)))
                .isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(abierto.cerrar(dec("0"), dec("10"), "x".repeat(1001), CIERRE)))
                .isEqualTo(VentasErrorCodes.OBSERVACION_INVALIDA);
        assertThat(value(abierto.cerrar(dec("0"), dec("10"), "x".repeat(1000), CIERRE)).observacionCierre())
                .hasSize(1000);
    }
}
