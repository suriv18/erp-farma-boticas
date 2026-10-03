package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.exception.VentasErrorCodes;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LineaVentaTest {

    private static final UUID ID = UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb");
    private static final UUID SKU = UUID.fromString("88888888-8888-4888-8888-888888888888");

    private static Result<LineaVenta, ErrorDetail> nueva(boolean permiteFraccion, BigDecimal cantidad, BigDecimal precio) {
        return LineaVenta.nueva(ID, 3, SKU, "Paracetamol 500 mg", "UND", permiteFraccion, cantidad, precio);
    }

    private static String code(Result<LineaVenta, ErrorDetail> result) {
        return result.fold(value -> { throw new AssertionError(value); }, ErrorDetail::code);
    }

    @Test
    void buildsALineWithTheRoundedTotal() {
        var linea = nueva(false, dec("5"), dec("2.50")).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(linea.id()).isEqualTo(ID);
        assertThat(linea.numeroLinea()).isEqualTo(3);
        assertThat(linea.skuId()).isEqualTo(SKU);
        assertThat(linea.descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(linea.unidadVentaCodigo()).isEqualTo("UND");
        assertThat(linea.esFraccion()).isFalse();
        assertThat(linea.cantidad()).isEqualTo(dec("5"));
        assertThat(linea.precioUnitario()).isEqualTo(dec("2.50"));
        assertThat(linea.totalLinea()).isEqualTo(dec("12.50"));
    }

    @Test
    void roundsTheLineTotalToTwoDecimalsHalfUp() {
        var linea = nueva(true, dec("3"), dec("0.3333")).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(linea.totalLinea()).isEqualTo(dec("1.00"));
    }

    @Test
    void aFractionalQuantityIsAllowedOnlyWhenTheSkuSellsByFraction() {
        var fraccion = nueva(true, dec("0.5"), dec("10")).fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(fraccion.esFraccion()).isTrue();
        assertThat(fraccion.totalLinea()).isEqualTo(dec("5.00"));
        assertThat(code(nueva(false, dec("0.5"), dec("10")))).isEqualTo(VentasErrorCodes.FRACCION_NO_PERMITIDA);
    }

    @Test
    void rejectsInvalidQuantities() {
        assertThat(code(nueva(false, null, dec("1")))).isEqualTo(VentasErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(nueva(false, dec("0"), dec("1")))).isEqualTo(VentasErrorCodes.CANTIDAD_INVALIDA);
        assertThat(code(nueva(true, dec("1.00001"), dec("1")))).isEqualTo(VentasErrorCodes.CANTIDAD_INVALIDA);
    }

    @Test
    void rejectsInvalidUnitPrices() {
        assertThat(code(nueva(false, dec("1"), null))).isEqualTo(VentasErrorCodes.PRECIO_INVALIDO);
        assertThat(code(nueva(false, dec("1"), dec("-0.01")))).isEqualTo(VentasErrorCodes.PRECIO_INVALIDO);
        assertThat(code(nueva(false, dec("1"), dec("1.00001")))).isEqualTo(VentasErrorCodes.PRECIO_INVALIDO);
    }
}
