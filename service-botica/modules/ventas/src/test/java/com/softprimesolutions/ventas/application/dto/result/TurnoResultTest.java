package com.softprimesolutions.ventas.application.dto.result;

import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TurnoResultTest {

    @Test
    void createsATurnoResult() {
        var result = new TurnoResult(
                TURNO, TERMINAL, ESTABLECIMIENTO, ACTOR_ID, AHORA, dec("50.00"), "ABIERTO", null,
                dec("0.00"), dec("50.00"), null, null, null);

        assertThat(result.id()).isEqualTo(TURNO);
        assertThat(result.terminalId()).isEqualTo(TERMINAL);
        assertThat(result.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(result.cajeroId()).isEqualTo(ACTOR_ID);
        assertThat(result.aperturaAt()).isEqualTo(AHORA);
        assertThat(result.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(result.estado()).isEqualTo("ABIERTO");
        assertThat(result.cierreAt()).isNull();
        assertThat(result.totalVentasSistema()).isEqualTo(dec("0.00"));
        assertThat(result.totalSistema()).isEqualTo(dec("50.00"));
        assertThat(result.totalDeclarado()).isNull();
        assertThat(result.diferencia()).isNull();
        assertThat(result.observacionCierre()).isNull();
    }
}
