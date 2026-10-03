package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import org.junit.jupiter.api.Test;

class NumeracionJdbcAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final NumeracionJdbcAdapter adapter = new NumeracionJdbcAdapter(jdbc.client());

    @Test
    void formatsTheNextTerminalSequenceWithSixDigits() {
        jdbc.scalar("ON CONFLICT (tenant_id, terminal_id)", 7L);

        assertThat(adapter.siguienteNumeroOperacion(TENANT, TERMINAL, "POS01")).isEqualTo("POS01-000007");
        assertThat(jdbc.statementContaining("ON CONFLICT (tenant_id, terminal_id)").params())
                .containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }
}
