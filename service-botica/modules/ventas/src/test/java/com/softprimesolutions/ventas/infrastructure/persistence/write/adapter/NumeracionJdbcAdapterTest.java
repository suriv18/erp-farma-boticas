package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import java.util.Map;
import org.junit.jupiter.api.Test;

class NumeracionJdbcAdapterTest {

    private static final String SIGUIENTE = "ON CONFLICT (tenant_id, terminal_id)";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final NumeracionJdbcAdapter adapter = new NumeracionJdbcAdapter(jdbc.client());

    @Test
    void prefixesTheNextTerminalSequenceWithTheEstablishmentAndTerminalCodes() {
        jdbc.rows(SIGUIENTE, Map.of(
                "establecimiento_codigo", "EST001", "terminal_codigo", "POS01", "ultimo_numero", 7L));

        assertThat(adapter.siguienteNumeroOperacion(TENANT, TERMINAL)).isEqualTo("EST001-POS01-000007");
        var statement = jdbc.statementContaining(SIGUIENTE);
        assertThat(statement.sql()).contains("JOIN sch_organizacion.establecimiento_farmaceutico");
        assertThat(statement.params()).containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }
}
