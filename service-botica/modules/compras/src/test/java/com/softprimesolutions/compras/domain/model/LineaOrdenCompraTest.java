package com.softprimesolutions.compras.domain.model;

import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.lineaOrden;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LineaOrdenCompraTest {

    @Test
    void computesTheGrossAmountRoundedToCents() {
        assertThat(LineaOrdenCompra.importeBruto(dec("3"), dec("1.005"))).isEqualByComparingTo("3.02");
        assertThat(lineaOrden("10", "0", "0", "0").importeBruto()).isEqualByComparingTo("55.00");
    }

    @Test
    void theOutstandingQuantityNeverGoesBelowZero() {
        assertThat(lineaOrden("10", "0", "0", "4").pendiente()).isEqualByComparingTo("6");
        assertThat(lineaOrden("10", "10", "0", "11").pendiente()).isEqualByComparingTo("0");
    }

    @Test
    void theMaximumReceivableAppliesTheExcessToleranceAndNeverGoesBelowZero() {
        assertThat(lineaOrden("10", "10", "0", "4").maximoRecibible()).isEqualByComparingTo("7");
        assertThat(lineaOrden("10", "0", "0", "10").maximoRecibible()).isEqualByComparingTo("0");
        assertThat(lineaOrden("10", "0", "0", "12").maximoRecibible()).isEqualByComparingTo("0");
    }

    @Test
    void aLineIsCompleteWhenTheReceivedQuantityReachesTheOrderedOneLessTheShortageTolerance() {
        assertThat(lineaOrden("10", "0", "0", "9").completa()).isFalse();
        assertThat(lineaOrden("10", "0", "0", "10").completa()).isTrue();
        assertThat(lineaOrden("10", "0", "10", "9").completa()).isTrue();
        assertThat(lineaOrden("10", "0", "10", "8.9").completa()).isFalse();
    }

    @Test
    void addingTheAcceptedQuantityKeepsTheRestOfTheLine() {
        var linea = lineaOrden("10", "10", "5", "2").conRecibido(dec("3"));

        assertThat(linea.cantidadRecibida()).isEqualByComparingTo("5");
        assertThat(linea.cantidad()).isEqualByComparingTo("10");
        assertThat(linea.toleranciaExcesoPct()).isEqualByComparingTo("10");
        assertThat(linea.toleranciaDefectoPct()).isEqualByComparingTo("5");
        assertThat(linea.numeroLinea()).isEqualTo(1);
    }
}
