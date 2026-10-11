package com.softprimesolutions.compras.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class CamposTest {

    @Test
    void normalizesTextToNullWhenBlankAndTrimsOtherwise() {
        assertThat(Campos.texto(null)).isNull();
        assertThat(Campos.texto("   ")).isNull();
        assertThat(Campos.texto("  abc ")).isEqualTo("abc");
    }

    @Test
    void detectsTextsLongerThanTheMaximum() {
        assertThat(Campos.excede(null, 3)).isFalse();
        assertThat(Campos.excede("abc", 3)).isFalse();
        assertThat(Campos.excede("abcd", 3)).isTrue();
    }

    @Test
    void detectsDecimalsWithTooManyDecimalPlacesOrAboveTheMaximum() {
        var maximo = new BigDecimal("100");
        assertThat(Campos.decimalInvalido(new BigDecimal("1.25"), 2, maximo)).isFalse();
        assertThat(Campos.decimalInvalido(new BigDecimal("1.2500"), 2, maximo)).isFalse();
        assertThat(Campos.decimalInvalido(new BigDecimal("1E+2"), 2, maximo)).isFalse();
        assertThat(Campos.decimalInvalido(new BigDecimal("1.255"), 2, maximo)).isTrue();
        assertThat(Campos.decimalInvalido(new BigDecimal("100.01"), 2, maximo)).isTrue();
    }
}
