package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class VentaTest {

    private static final UUID VENTA = UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa");
    private static final UUID SKU = UUID.fromString("88888888-8888-4888-8888-888888888888");

    private static LineaVenta linea(int numero, String cantidad, String precio) {
        return LineaVenta.nueva(UUID.randomUUID(), numero, SKU, "Producto", "UND", false, dec(cantidad), dec(precio))
                .fold(value -> value, error -> { throw new AssertionError(error); });
    }

    private static Result<Venta, ErrorDetail> registrar(List<LineaVenta> lineas, BigDecimal recibido) {
        return Venta.registrar(
                VENTA, TENANT, TERMINAL, TURNO, ESTABLECIMIENTO, ACTOR, "POS01-000001", AHORA, lineas, recibido);
    }

    private static String code(Result<Venta, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    @Test
    void registersASaleWithTheSumOfItsLinesAsSubtotalAndTotal() {
        var venta = registrar(List.of(linea(1, "5", "2.50"), linea(2, "2", "10.00")), dec("50"))
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(venta.id()).isEqualTo(VENTA);
        assertThat(venta.tenantId()).isEqualTo(TENANT);
        assertThat(venta.terminalId()).isEqualTo(TERMINAL);
        assertThat(venta.turnoId()).isEqualTo(TURNO);
        assertThat(venta.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(venta.vendedor()).isEqualTo(ACTOR);
        assertThat(venta.numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(venta.fechaVenta()).isEqualTo(AHORA);
        assertThat(venta.lineas()).hasSize(2);
        assertThat(venta.subtotal()).isEqualTo(dec("32.50"));
        assertThat(venta.total()).isEqualTo(dec("32.50"));
        assertThat(venta.pago().montoRecibido()).isEqualTo(dec("50.00"));
        assertThat(venta.pago().vuelto()).isEqualTo(dec("17.50"));
    }

    @Test
    void aSaleNeedsBetweenOneAndOneHundredLines() {
        assertThat(code(registrar(List.of(), dec("10")))).isEqualTo(VentasErrorCodes.VENTA_SIN_LINEAS);
        var demasiadas = IntStream.rangeClosed(1, 101).mapToObj(numero -> linea(numero, "1", "1")).toList();
        assertThat(code(registrar(demasiadas, dec("500")))).isEqualTo(VentasErrorCodes.VENTA_LINEAS_EXCEDIDAS);
        var cien = IntStream.rangeClosed(1, 100).mapToObj(numero -> linea(numero, "1", "1")).toList();
        assertThat(registrar(cien, dec("100")).isSuccess()).isTrue();
    }

    @Test
    void aSaleWithAZeroTotalIsRejected() {
        assertThat(code(registrar(List.of(linea(1, "1", "0")), dec("10")))).isEqualTo(VentasErrorCodes.TOTAL_INVALIDO);
    }

    @Test
    void theCashPaymentIsValidatedAgainstTheTotal() {
        assertThat(code(registrar(List.of(linea(1, "1", "10")), null))).isEqualTo(VentasErrorCodes.MONTO_INVALIDO);
        assertThat(code(registrar(List.of(linea(1, "1", "10")), dec("9.99"))))
                .isEqualTo(VentasErrorCodes.MONTO_RECIBIDO_INSUFICIENTE);
    }

    @Test
    void theLinesAreImmutable() {
        var venta = registrar(List.of(linea(1, "1", "10")), dec("10"))
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThatThrownBy(() -> venta.lineas().add(linea(2, "1", "1")))
                .isInstanceOf(UnsupportedOperationException.class);
    }
}
