package com.softprimesolutions.ventas.domain.model;

import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;
import org.junit.jupiter.api.Test;

class LoteConsumoTest {

    @Test
    void exposesTheLotAndTheConsumedQuantity() {
        var lote = UUID.fromString("99999999-9999-4999-8999-999999999999");

        var consumo = new LoteConsumo(lote, dec("2"));

        assertThat(consumo.loteId()).isEqualTo(lote);
        assertThat(consumo.cantidad()).isEqualTo(dec("2"));
    }
}
