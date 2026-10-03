package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ReferenciasVentasJdbcAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final ReferenciasVentasJdbcAdapter adapter = new ReferenciasVentasJdbcAdapter(jdbc.client());

    @Test
    void findsATerminalWithItsEstablishmentAndOperability() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", TERMINAL);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("codigo", "POS01");
        row.put("operable", true);
        jdbc.rows("FROM sch_organizacion.terminal_pos tp", row);

        var terminal = adapter.terminal(TENANT, TERMINAL).orElseThrow();

        assertThat(terminal.id()).isEqualTo(TERMINAL);
        assertThat(terminal.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(terminal.codigo()).isEqualTo("POS01");
        assertThat(terminal.operable()).isTrue();
        assertThat(jdbc.statementContaining("FROM sch_organizacion.terminal_pos tp").params())
                .containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }

    @Test
    void anUnknownTerminalIsEmpty() {
        assertThat(adapter.terminal(TENANT, TERMINAL)).isEmpty();
    }
}
