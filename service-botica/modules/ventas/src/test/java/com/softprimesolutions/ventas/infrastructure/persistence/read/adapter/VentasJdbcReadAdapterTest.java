package com.softprimesolutions.ventas.infrastructure.persistence.read.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.ventas.infrastructure.persistence.Rows;
import org.junit.jupiter.api.Test;

class VentasJdbcReadAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final VentasJdbcReadAdapter adapter = new VentasJdbcReadAdapter(jdbc.client());

    @Test
    void findsATurnoByIdAsAResult() {
        jdbc.rows("tc.uuid_publico = :turnoId", Rows.turno());

        var turno = adapter.findTurno(TENANT, TURNO).orElseThrow();

        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(turno.terminalId()).isEqualTo(TERMINAL);
        assertThat(turno.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(turno.cajeroId()).isEqualTo(ACTOR_ID);
        assertThat(turno.estado()).isEqualTo("ABIERTO");
        assertThat(turno.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(jdbc.statementContaining("tc.uuid_publico = :turnoId").params())
                .containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO);
    }

    @Test
    void findsTheOpenTurnoOfATerminalIncludingOnesBeingCounted() {
        jdbc.rows("tc.estado IN ('ABIERTO', 'EN_ARQUEO')", Rows.turno());

        var turno = adapter.findTurnoAbierto(TENANT, TERMINAL).orElseThrow();

        assertThat(turno.id()).isEqualTo(TURNO);
        assertThat(jdbc.statementContaining("tc.estado IN ('ABIERTO', 'EN_ARQUEO')").params())
                .containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }

    @Test
    void missingTurnosAreEmpty() {
        assertThat(adapter.findTurno(TENANT, TURNO)).isEmpty();
        assertThat(adapter.findTurnoAbierto(TENANT, TERMINAL)).isEmpty();
    }
}
