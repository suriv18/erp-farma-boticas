package com.softprimesolutions.ventas.api.mapper;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import org.junit.jupiter.api.Test;

class VentasApiMapperTest {

    @Test
    void mapsTheOpenRequestToACommandWithTheTenantAndActorFromTheToken() {
        var command = VentasApiMapper.toCommand(TENANT, ACTOR_ID, new AbrirTurnoRequest(TERMINAL, dec("50")));

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.terminalId()).isEqualTo(TERMINAL);
        assertThat(command.fondoInicial()).isEqualTo(dec("50"));
    }

    @Test
    void mapsTheCloseRequestToACommand() {
        var command = VentasApiMapper.toCommand(
                TENANT, ACTOR_ID, TURNO, new CerrarTurnoRequest(dec("135"), "Cierre"));

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.turnoId()).isEqualTo(TURNO);
        assertThat(command.totalDeclarado()).isEqualTo(dec("135"));
        assertThat(command.observacion()).isEqualTo("Cierre");
    }

    @Test
    void mapsATurnoResultToItsResponse() {
        var response = VentasApiMapper.toResponse(turnoResult());

        assertThat(response.id()).isEqualTo(TURNO);
        assertThat(response.terminalId()).isEqualTo(TERMINAL);
        assertThat(response.cajeroId()).isEqualTo(ACTOR_ID);
        assertThat(response.estado()).isEqualTo("ABIERTO");
        assertThat(response.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(response.totalSistema()).isEqualTo(dec("50.00"));
        assertThat(response.totalDeclarado()).isNull();
    }
}
