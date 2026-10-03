package com.softprimesolutions.ventas.api.controller;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.conflict;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ok;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.ventas.api.dto.request.LineaVentaRequest;
import com.softprimesolutions.ventas.api.dto.request.PagoEfectivoRequest;
import com.softprimesolutions.ventas.api.dto.request.VentaRequest;
import com.softprimesolutions.ventas.api.dto.response.PaginaResponse;
import com.softprimesolutions.ventas.api.dto.response.VentaResponse;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

class VentaControllerTest {

    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "none")
            .subject(ACTOR_ID.toString()).claim("tid", TENANT.toString()).build();

    private final ConsultarVentasUseCase consultas = mock(ConsultarVentasUseCase.class);

    private static VentaRequest request() {
        return new VentaRequest(
                TERMINAL, ALMACEN, List.of(new LineaVentaRequest(SKU, dec("5"), dec("2.50"))),
                new PagoEfectivoRequest(dec("20")));
    }

    private static void assertConflict(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    }

    @Test
    void registersASaleWithTheKeyTenantAndActorAndAnswersCreated() {
        var received = new AtomicReference<RegistrarVentaCommand>();
        var controller = new VentaController(command -> {
            received.set(command);
            return ok(ventaResult());
        }, consultas);

        var response = controller.register(JWT, "clave-1", request());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(((VentaResponse) response.getBody()).id()).isEqualTo(VENTA);
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().idempotencyKey()).isEqualTo("clave-1");
        assertThat(received.get().terminalId()).isEqualTo(TERMINAL);
    }

    @Test
    void aMissingKeyIsPassedAsNullSoTheUseCaseRejectsIt() {
        var received = new AtomicReference<RegistrarVentaCommand>();
        var controller = new VentaController(command -> {
            received.set(command);
            return conflict();
        }, consultas);

        assertConflict(controller.register(JWT, null, request()));
        assertThat(received.get().idempotencyKey()).isNull();
    }

    @Test
    void getsASaleById() {
        when(consultas.obtener(new ObtenerVentaQuery(TENANT, VENTA))).thenReturn(ok(ventaResult()));
        var controller = new VentaController(command -> conflict(), consultas);

        var response = controller.getById(JWT, VENTA);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(((VentaResponse) response.getBody()).numeroOperacion()).isEqualTo("EST001-POS01-000001");
    }

    @Test
    void aMissingSaleBecomesAProblem() {
        when(consultas.obtener(new ObtenerVentaQuery(TENANT, VENTA))).thenReturn(conflict());
        var controller = new VentaController(command -> conflict(), consultas);

        assertConflict(controller.getById(JWT, VENTA));
    }

    @Test
    void listsSalesWithTheFiltersAndPagination() {
        var query = new ListarVentasQuery(TENANT, ESTABLECIMIENTO, AHORA, AHORA.plusSeconds(60), 1, 10);
        when(consultas.listar(query)).thenReturn(ok(new PaginaResult<>(List.of(), 1, 10, 0L)));
        var controller = new VentaController(command -> conflict(), consultas);

        var response = controller.list(JWT, ESTABLECIMIENTO, AHORA, AHORA.plusSeconds(60), 1, 10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var pagina = (PaginaResponse<?>) response.getBody();
        assertThat(pagina.page()).isEqualTo(1);
        assertThat(pagina.size()).isEqualTo(10);
    }

    @Test
    void aListFailureBecomesAProblem() {
        when(consultas.listar(new ListarVentasQuery(TENANT, null, null, null, 0, 500))).thenReturn(conflict());
        var controller = new VentaController(command -> conflict(), consultas);

        assertConflict(controller.list(JWT, null, null, null, 0, 500));
    }
}
