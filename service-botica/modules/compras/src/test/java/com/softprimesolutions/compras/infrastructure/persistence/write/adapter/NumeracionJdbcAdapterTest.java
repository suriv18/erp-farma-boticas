package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.infrastructure.persistence.JdbcClientStub;
import org.junit.jupiter.api.Test;

class NumeracionJdbcAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub()
            .scalar("seq_orden_compra_numero", "OC-2026-000007")
            .scalar("seq_recepcion_compra_numero", "REC-2026-000003");
    private final NumeracionJdbcAdapter adapter = new NumeracionJdbcAdapter(jdbc.client());

    @Test
    void takesTheNextOrderNumberFromItsSequence() {
        assertThat(adapter.siguienteNumeroOrden()).isEqualTo("OC-2026-000007");
    }

    @Test
    void takesTheNextReceptionNumberFromItsSequence() {
        assertThat(adapter.siguienteNumeroRecepcion()).isEqualTo("REC-2026-000003");
    }
}
