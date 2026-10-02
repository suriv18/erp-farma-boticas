package com.softprimesolutions.compras.api.controller;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.VENCIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.conflict;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.ok;
import static com.softprimesolutions.compras.ComprasFixtures.ordenNueva;
import static com.softprimesolutions.compras.ComprasFixtures.proveedor;
import static com.softprimesolutions.compras.ComprasResultFixtures.ordenResumen;
import static com.softprimesolutions.compras.ComprasResultFixtures.recepcionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.compras.api.dto.request.AnularOrdenCompraRequest;
import com.softprimesolutions.compras.api.dto.request.CambiarEstadoProveedorRequest;
import com.softprimesolutions.compras.api.dto.request.ItemRecepcionRequest;
import com.softprimesolutions.compras.api.dto.request.LineaOrdenCompraRequest;
import com.softprimesolutions.compras.api.dto.request.OrdenCompraRequest;
import com.softprimesolutions.compras.api.dto.request.ProveedorRequest;
import com.softprimesolutions.compras.api.dto.request.RecepcionRequest;
import com.softprimesolutions.compras.api.dto.response.OrdenCompraResponse;
import com.softprimesolutions.compras.api.dto.response.PaginaResponse;
import com.softprimesolutions.compras.api.dto.response.ProveedorResponse;
import com.softprimesolutions.compras.api.dto.response.RecepcionResponse;
import com.softprimesolutions.compras.application.dto.command.ActualizarProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.CambiarEstadoProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.CrearOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.command.CrearProveedorCommand;
import com.softprimesolutions.compras.application.dto.command.RegistrarRecepcionCommand;
import com.softprimesolutions.compras.application.dto.command.TransicionOrden;
import com.softprimesolutions.compras.application.dto.command.TransicionarOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.query.ListarOrdenesCompraQuery;
import com.softprimesolutions.compras.application.dto.query.ListarProveedoresQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerOrdenCompraQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerProveedorQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerRecepcionQuery;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.application.port.in.ConsultarOrdenesCompraUseCase;
import com.softprimesolutions.compras.application.port.in.ConsultarProveedoresUseCase;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

class ComprasControllersTest {

    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "none")
            .subject(ACTOR_ID.toString()).claim("tid", TENANT.toString()).build();

    private static void assertConflict(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    }

    private static ProveedorRequest proveedorRequest() {
        return new ProveedorRequest(
                null, "20100070970", "Laboratorios SAC", null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null);
    }

    private static ConsultarProveedoresUseCase proveedoresQuery() {
        return mock(ConsultarProveedoresUseCase.class);
    }

    private static ProveedorController proveedorController(
            AtomicReference<Object> received, boolean fails, ConsultarProveedoresUseCase query) {
        return new ProveedorController(
                command -> {
                    received.set(command);
                    return fails ? conflict() : ok(ComprasApplicationMapper.toResult(proveedor(EstadoProveedor.ACTIVO)));
                },
                command -> {
                    received.set(command);
                    return fails ? conflict() : ok(ComprasApplicationMapper.toResult(proveedor(EstadoProveedor.ACTIVO)));
                },
                command -> {
                    received.set(command);
                    return fails ? conflict() : ok(ComprasApplicationMapper.toResult(proveedor(EstadoProveedor.BLOQUEADO)));
                },
                query);
    }

    @Test
    void createsAProveedorForTheTenantAndActorOfTheToken() {
        var received = new AtomicReference<Object>();

        var response = proveedorController(received, false, proveedoresQuery()).create(JWT, proveedorRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOfSatisfying(ProveedorResponse.class,
                body -> assertThat(body.id()).isEqualTo(PROVEEDOR));
        var command = (CrearProveedorCommand) received.get();
        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.proveedor().numeroDocumento()).isEqualTo("20100070970");
    }

    @Test
    void updatesAProveedor() {
        var received = new AtomicReference<Object>();

        var response = proveedorController(received, false, proveedoresQuery())
                .update(JWT, PROVEEDOR, proveedorRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var command = (ActualizarProveedorCommand) received.get();
        assertThat(command.proveedorId()).isEqualTo(PROVEEDOR);
        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void changesTheStateOfAProveedor() {
        var received = new AtomicReference<Object>();

        var response = proveedorController(received, false, proveedoresQuery())
                .changeEstado(JWT, PROVEEDOR, new CambiarEstadoProveedorRequest("BLOQUEADO"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        var command = (CambiarEstadoProveedorCommand) received.get();
        assertThat(command.estado()).isEqualTo("BLOQUEADO");
        assertThat(command.proveedorId()).isEqualTo(PROVEEDOR);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void translatesTheProveedorCommandFailuresToProblemDetails() {
        var received = new AtomicReference<Object>();
        var controller = proveedorController(received, true, proveedoresQuery());

        assertConflict(controller.create(JWT, proveedorRequest()));
        assertConflict(controller.update(JWT, PROVEEDOR, proveedorRequest()));
        assertConflict(controller.changeEstado(JWT, PROVEEDOR, new CambiarEstadoProveedorRequest("ACTIVO")));
    }

    @Test
    void getsAndListsProveedores() {
        var query = proveedoresQuery();
        var result = ComprasApplicationMapper.toResult(proveedor(EstadoProveedor.ACTIVO));
        when(query.obtener(new ObtenerProveedorQuery(TENANT, PROVEEDOR))).thenReturn(ok(result));
        when(query.listar(new ListarProveedoresQuery(TENANT, "ACTIVO", "lab", 1, 50)))
                .thenReturn(ok(new PaginaResult<>(List.of(result), 1, 50, 51)));
        var controller = proveedorController(new AtomicReference<>(), false, query);

        var one = controller.getById(JWT, PROVEEDOR);
        var page = controller.list(JWT, "ACTIVO", "lab", 1, 50);

        assertThat(one.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(one.getBody()).isInstanceOfSatisfying(ProveedorResponse.class,
                body -> assertThat(body.razonSocial()).isEqualTo("Laboratorios SAC"));
        assertThat(page.getBody()).isInstanceOfSatisfying(PaginaResponse.class,
                body -> assertThat(body.totalElements()).isEqualTo(51));
    }

    @Test
    void translatesTheProveedorQueryFailuresToProblemDetails() {
        var query = proveedoresQuery();
        when(query.obtener(new ObtenerProveedorQuery(TENANT, PROVEEDOR))).thenReturn(conflict());
        when(query.listar(new ListarProveedoresQuery(TENANT, null, null, 0, 20))).thenReturn(conflict());
        var controller = proveedorController(new AtomicReference<>(), false, query);

        assertConflict(controller.getById(JWT, PROVEEDOR));
        assertConflict(controller.list(JWT, null, null, 0, 20));
    }

    private static OrdenCompraRequest ordenRequest() {
        return new OrdenCompraRequest(
                PROVEEDOR, ESTABLECIMIENTO, LocalDate.of(2026, 7, 1), null, null, null, null, null,
                List.of(new LineaOrdenCompraRequest(SKU, dec("10"), "UND", dec("5"), null, null, null, null)));
    }

    private static OrdenCompraController ordenController(
            AtomicReference<Object> received, boolean fails, ConsultarOrdenesCompraUseCase query) {
        return new OrdenCompraController(
                command -> {
                    received.set(command);
                    return fails ? conflict() : ok(ComprasApplicationMapper.toResult(ordenNueva()));
                },
                command -> {
                    received.set(command);
                    return fails ? conflict() : ok(ComprasApplicationMapper.toResult(ordenNueva()));
                },
                query);
    }

    @Test
    void createsAnOrderForTheTenantAndActorOfTheToken() {
        var received = new AtomicReference<Object>();

        var response = ordenController(received, false, mock(ConsultarOrdenesCompraUseCase.class))
                .create(JWT, ordenRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOfSatisfying(OrdenCompraResponse.class,
                body -> assertThat(body.id()).isEqualTo(ORDEN));
        var command = (CrearOrdenCompraCommand) received.get();
        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.proveedorId()).isEqualTo(PROVEEDOR);
    }

    @Test
    void approvesIssuesAndCancelsAnOrderThroughItsTransitions() {
        var received = new AtomicReference<Object>();
        var controller = ordenController(received, false, mock(ConsultarOrdenesCompraUseCase.class));

        assertThat(controller.approve(JWT, ORDEN).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received.get()).isEqualTo(new TransicionarOrdenCompraCommand(
                TENANT, ORDEN, TransicionOrden.APROBAR, null, ACTOR_ID));
        assertThat(controller.issue(JWT, ORDEN).getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received.get()).isEqualTo(new TransicionarOrdenCompraCommand(
                TENANT, ORDEN, TransicionOrden.EMITIR, null, ACTOR_ID));
        assertThat(controller.cancel(JWT, ORDEN, new AnularOrdenCompraRequest("Error")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
        assertThat(received.get()).isEqualTo(new TransicionarOrdenCompraCommand(
                TENANT, ORDEN, TransicionOrden.ANULAR, "Error", ACTOR_ID));
    }

    @Test
    void translatesTheOrderCommandFailuresToProblemDetails() {
        var controller = ordenController(new AtomicReference<>(), true, mock(ConsultarOrdenesCompraUseCase.class));

        assertConflict(controller.create(JWT, ordenRequest()));
        assertConflict(controller.approve(JWT, ORDEN));
    }

    @Test
    void getsAndListsOrders() {
        var query = mock(ConsultarOrdenesCompraUseCase.class);
        when(query.obtener(new ObtenerOrdenCompraQuery(TENANT, ORDEN)))
                .thenReturn(ok(ComprasApplicationMapper.toResult(ordenNueva())));
        when(query.listar(new ListarOrdenesCompraQuery(TENANT, PROVEEDOR, "EMITIDA", 0, 20)))
                .thenReturn(ok(new PaginaResult<>(List.of(ordenResumen()), 0, 20, 1)));
        var controller = ordenController(new AtomicReference<>(), false, query);

        var one = controller.getById(JWT, ORDEN);
        var page = controller.list(JWT, PROVEEDOR, "EMITIDA", 0, 20);

        assertThat(one.getBody()).isInstanceOfSatisfying(OrdenCompraResponse.class,
                body -> assertThat(body.numero()).isEqualTo("OC-2026-000001"));
        assertThat(page.getBody()).isInstanceOfSatisfying(PaginaResponse.class,
                body -> assertThat(body.totalElements()).isEqualTo(1));
    }

    @Test
    void translatesTheOrderQueryFailuresToProblemDetails() {
        var query = mock(ConsultarOrdenesCompraUseCase.class);
        when(query.obtener(new ObtenerOrdenCompraQuery(TENANT, ORDEN))).thenReturn(conflict());
        when(query.listar(new ListarOrdenesCompraQuery(TENANT, null, null, 0, 20))).thenReturn(conflict());
        var controller = ordenController(new AtomicReference<>(), false, query);

        assertConflict(controller.getById(JWT, ORDEN));
        assertConflict(controller.list(JWT, null, null, 0, 20));
    }

    private static RecepcionRequest recepcionRequest() {
        return new RecepcionRequest(
                ORDEN, ALMACEN, null, null, null, null, null, null, null, null,
                List.of(new ItemRecepcionRequest(
                        1, "LOTE-1", null, VENCIMIENTO, dec("6"), dec("1"), "Roto", null, null)));
    }

    @Test
    void registersAReceptionWithTheIdempotencyKeyOfTheRequest() {
        var received = new AtomicReference<RegistrarRecepcionCommand>();
        var controller = new RecepcionController(command -> {
            received.set(command);
            return ok(recepcionResult());
        }, query -> ok(recepcionResult()));

        var response = controller.register(JWT, "clave-1", recepcionRequest());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOfSatisfying(RecepcionResponse.class,
                body -> assertThat(body.id()).isEqualTo(RECEPCION));
        assertThat(received.get().idempotencyKey()).isEqualTo("clave-1");
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().items()).hasSize(1);
    }

    @Test
    void getsAReceptionById() {
        var received = new AtomicReference<ObtenerRecepcionQuery>();
        var controller = new RecepcionController(command -> conflict(), query -> {
            received.set(query);
            return ok(recepcionResult());
        });

        var response = controller.getById(JWT, RECEPCION);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received.get()).isEqualTo(new ObtenerRecepcionQuery(TENANT, RECEPCION));
    }

    @Test
    void translatesTheReceptionFailuresToProblemDetails() {
        var controller = new RecepcionController(command -> conflict(), query -> conflict());

        assertConflict(controller.register(JWT, null, recepcionRequest()));
        assertConflict(controller.getById(JWT, UUID.randomUUID()));
    }
}
