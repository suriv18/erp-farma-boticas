package com.softprimesolutions.ventas.domain.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class ImportesTest {

    @Test
    void acceptsAmountsFromZeroUpToTheMaximumWithAtMostTwoDecimals() {
        assertThat(Importes.montoValido(new BigDecimal("0"))).isTrue();
        assertThat(Importes.montoValido(new BigDecimal("12.50"))).isTrue();
        assertThat(Importes.montoValido(new BigDecimal("12.500"))).isTrue();
        assertThat(Importes.montoValido(new BigDecimal("1000000000"))).isTrue();
        assertThat(Importes.montoValido(new BigDecimal("1E+3"))).isTrue();
    }

    @Test
    void rejectsMissingNegativeTooPreciseOrTooLargeAmounts() {
        assertThat(Importes.montoValido(null)).isFalse();
        assertThat(Importes.montoValido(new BigDecimal("-0.01"))).isFalse();
        assertThat(Importes.montoValido(new BigDecimal("1.001"))).isFalse();
        assertThat(Importes.montoValido(new BigDecimal("1000000000.01"))).isFalse();
    }

    @Test
    void validatesUnitPricesWithUpToFourDecimalsAndNeverNegative() {
        assertThat(Importes.precioValido(new BigDecimal("0"))).isTrue();
        assertThat(Importes.precioValido(new BigDecimal("2.5001"))).isTrue();
        assertThat(Importes.precioValido(null)).isFalse();
        assertThat(Importes.precioValido(new BigDecimal("-0.0001"))).isFalse();
        assertThat(Importes.precioValido(new BigDecimal("1.00001"))).isFalse();
        assertThat(Importes.precioValido(new BigDecimal("1000000000.0001"))).isFalse();
    }

    @Test
    void validatesQuantitiesAsPositiveWithUpToFourDecimals() {
        assertThat(Importes.cantidadValida(new BigDecimal("0.0001"))).isTrue();
        assertThat(Importes.cantidadValida(new BigDecimal("12"))).isTrue();
        assertThat(Importes.cantidadValida(null)).isFalse();
        assertThat(Importes.cantidadValida(BigDecimal.ZERO)).isFalse();
        assertThat(Importes.cantidadValida(new BigDecimal("-1"))).isFalse();
        assertThat(Importes.cantidadValida(new BigDecimal("1.00001"))).isFalse();
        assertThat(Importes.cantidadValida(new BigDecimal("1000000000.0001"))).isFalse();
    }

    @Test
    void roundsToTwoDecimalsHalfUp() {
        assertThat(Importes.redondear(new BigDecimal("1.005"))).isEqualTo(new BigDecimal("1.01"));
        assertThat(Importes.redondear(new BigDecimal("7"))).isEqualTo(new BigDecimal("7.00"));
    }
}
