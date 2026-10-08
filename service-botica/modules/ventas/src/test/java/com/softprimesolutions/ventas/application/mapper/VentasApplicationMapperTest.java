package com.softprimesolutions.ventas.application.mapper;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import org.junit.jupiter.api.Test;

class VentasApplicationMapperTest {

    @Test
    void mapsATurnoToItsResult() {
        var result = VentasApplicationMapper.toResult(turno(EstadoTurno.ABIERTO, "50.00"));

        assertThat(result.id()).isEqualTo(TURNO);
        assertThat(result.terminalId()).isEqualTo(TERMINAL);
        assertThat(result.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(result.cajeroId()).isEqualTo(ACTOR_ID);
        assertThat(result.aperturaAt()).isEqualTo(AHORA);
        assertThat(result.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(result.estado()).isEqualTo("ABIERTO");
        assertThat(result.totalVentasSistema()).isEqualTo(dec("0.00"));
        assertThat(result.totalSistema()).isEqualTo(dec("50.00"));
        assertThat(result.cierreAt()).isNull();
        assertThat(result.totalDeclarado()).isNull();
        assertThat(result.diferencia()).isNull();
        assertThat(result.observacionCierre()).isNull();
    }
}
