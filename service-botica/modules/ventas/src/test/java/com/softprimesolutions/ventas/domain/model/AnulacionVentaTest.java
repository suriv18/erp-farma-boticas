package com.softprimesolutions.ventas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import org.junit.jupiter.api.Test;

class AnulacionVentaTest {

    private static String code(Result<String, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    @Test
    void acceptsAConfirmedSaleOfAnOpenTurnoAndReturnsTheTrimmedReason() {
        var motivo = AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, "  Error de cobro  ")
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(motivo).isEqualTo("Error de cobro");
    }

    @Test
    void theReasonIsRequiredAndAtMostFiveHundredCharacters() {
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, null)))
                .isEqualTo(VentasErrorCodes.MOTIVO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, "   ")))
                .isEqualTo(VentasErrorCodes.MOTIVO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, "x".repeat(501))))
                .isEqualTo(VentasErrorCodes.MOTIVO_INVALIDO);
        assertThat(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.ABIERTO, "x".repeat(500)).isSuccess())
                .isTrue();
    }

    @Test
    void onlyAConfirmedSaleCanBeAnnulled() {
        assertThat(code(AnulacionVenta.validar(EstadoVenta.ANULADA, EstadoTurno.ABIERTO, "motivo")))
                .isEqualTo(VentasErrorCodes.VENTA_ESTADO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.DEVUELTA, EstadoTurno.ABIERTO, "motivo")))
                .isEqualTo(VentasErrorCodes.VENTA_ESTADO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.PARCIALMENTE_DEVUELTA, EstadoTurno.ABIERTO, "motivo")))
                .isEqualTo(VentasErrorCodes.VENTA_ESTADO_INVALIDO);
    }

    @Test
    void theTurnoOfTheSaleMustStillBeOpen() {
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.CERRADO, "motivo")))
                .isEqualTo(VentasErrorCodes.TURNO_NO_ABIERTO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.CONFIRMADA, EstadoTurno.EN_ARQUEO, "motivo")))
                .isEqualTo(VentasErrorCodes.TURNO_NO_ABIERTO);
    }

    @Test
    void theReasonIsValidatedBeforeTheStates() {
        assertThat(code(AnulacionVenta.validar(EstadoVenta.ANULADA, EstadoTurno.CERRADO, " ")))
                .isEqualTo(VentasErrorCodes.MOTIVO_INVALIDO);
        assertThat(code(AnulacionVenta.validar(EstadoVenta.ANULADA, EstadoTurno.CERRADO, "motivo")))
                .isEqualTo(VentasErrorCodes.VENTA_ESTADO_INVALIDO);
    }
}
