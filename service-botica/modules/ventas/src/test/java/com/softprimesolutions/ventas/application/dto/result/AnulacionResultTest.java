package com.softprimesolutions.ventas.application.dto.result;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ventaAnuladaResult;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AnulacionResultTest {

    @Test
    void anAnnulledSaleCarriesWhenWhoAndWhyItWasAnnulled() {
        var venta = ventaAnuladaResult();

        assertThat(venta.estado()).isEqualTo("ANULADA");
        assertThat(venta.anulacion()).isEqualTo(new AnulacionResult(AHORA, ACTOR_ID, "Error de cobro"));
        assertThat(venta.anulacion().anuladaAt()).isEqualTo(AHORA);
        assertThat(venta.anulacion().anuladaPorId()).isEqualTo(ACTOR_ID);
        assertThat(venta.anulacion().motivo()).isEqualTo("Error de cobro");
    }
}
