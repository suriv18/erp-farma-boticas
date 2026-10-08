package com.softprimesolutions.ventas.api.controller;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.conflict;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import com.softprimesolutions.ventas.api.dto.response.TurnoResponse;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.query.ObtenerTurnoQuery;
import com.softprimesolutions.ventas.application.dto.query.TurnoActualQuery;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

class TurnoControllerTest {

    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "none")
            .subject(ACTOR_ID.toString()).claim("tid", TENANT.toString()).build();

    private final ConsultarTurnosUseCase consultas = mock(ConsultarTurnosUseCase.class);

    private static void assertConflict(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    }

    @Test
    void opensATurnoWithTheTenantAndActorOfTheTokenAndAnswersCreated() {
        var received = new AtomicReference<AbrirTurnoCommand>();
        var controller = new TurnoController(command -> {
            received.set(command);
            return ok(turnoResult());
        }, command -> conflict(), consultas);

        var response = controller.open(JWT, new AbrirTurnoRequest(TERMINAL, dec("50")));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((TurnoResponse) response.getBody()).id()).isEqualTo(TURNO);
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().terminalId()).isEqualTo(TERMINAL);
        assertThat(received.get().fondoInicial()).isEqualTo(dec("50"));
    }

    @Test
    void anOpenFailureBecomesAProblem() {
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.open(JWT, new AbrirTurnoRequest(TERMINAL, dec("50"))));
    }

    @Test
    void closesATurnoAndAnswersOk() {
        var received = new AtomicReference<CerrarTurnoCommand>();
        var controller = new TurnoController(command -> conflict(), command -> {
            received.set(command);
            return ok(turnoResult());
        }, consultas);

        var response = controller.close(JWT, TURNO, new CerrarTurnoRequest(dec("135"), "Cierre"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((TurnoResponse) response.getBody()).id()).isEqualTo(TURNO);
        assertThat(received.get().turnoId()).isEqualTo(TURNO);
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().totalDeclarado()).isEqualTo(dec("135"));
    }

    @Test
    void aCloseFailureBecomesAProblem() {
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.close(JWT, TURNO, new CerrarTurnoRequest(dec("135"), null)));
    }

    @Test
    void getsATurnoById() {
        when(consultas.obtener(new ObtenerTurnoQuery(TENANT, TURNO))).thenReturn(ok(turnoResult()));
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        var response = controller.getById(JWT, TURNO);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((TurnoResponse) response.getBody()).id()).isEqualTo(TURNO);
    }

    @Test
    void aMissingTurnoByIdBecomesAProblem() {
        when(consultas.obtener(new ObtenerTurnoQuery(TENANT, TURNO))).thenReturn(conflict());
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.getById(JWT, TURNO));
    }

    @Test
    void getsTheCurrentTurnoOfATerminal() {
        when(consultas.actual(new TurnoActualQuery(TENANT, TERMINAL))).thenReturn(ok(turnoResult()));
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        var response = controller.current(JWT, TERMINAL);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((TurnoResponse) response.getBody()).terminalId()).isEqualTo(TERMINAL);
    }

    @Test
    void aTerminalWithoutAnOpenTurnoBecomesAProblem() {
        when(consultas.actual(new TurnoActualQuery(TENANT, TERMINAL))).thenReturn(conflict());
        var controller = new TurnoController(command -> conflict(), command -> conflict(), consultas);

        assertConflict(controller.current(JWT, TERMINAL));
    }
}
