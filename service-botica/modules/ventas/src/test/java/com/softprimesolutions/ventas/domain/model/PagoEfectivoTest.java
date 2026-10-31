package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import org.junit.jupiter.api.Test;

class PagoEfectivoTest {

    private static String code(Result<PagoEfectivo, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    @Test
    void chargesTheTotalAndReturnsTheChange() {
        var pago = PagoEfectivo.cobrar(dec("12.50"), dec("20"))
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(pago.monto()).isEqualTo(dec("12.50"));
        assertThat(pago.montoRecibido()).isEqualTo(dec("20.00"));
        assertThat(pago.vuelto()).isEqualTo(dec("7.50"));
    }

    @Test
    void anExactAmountHasNoChange() {
        var pago = PagoEfectivo.cobrar(dec("12.50"), dec("12.50"))
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(pago.vuelto()).isEqualTo(dec("0.00"));
    }

    @Test
    void rejectsAnInvalidOrInsufficientReceivedAmount() {
        assertThat(code(PagoEfectivo.cobrar(dec("12.50"), null))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(PagoEfectivo.cobrar(dec("12.50"), dec("-1")))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(PagoEfectivo.cobrar(dec("12.50"), dec("12.49"))))
                .isEqualTo(VentasErrorCodes.MONTO_RECIBIDO_INSUFICIENTE);
    }
}
