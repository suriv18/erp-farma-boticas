package com.softprimesolutions.inventario.api.controller;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.conflict;
import static com.softprimesolutions.inventario.InventarioFixtures.loteResult;
import static com.softprimesolutions.inventario.InventarioFixtures.ok;
import static com.softprimesolutions.inventario.InventarioFixtures.paginaPosiciones;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.api.dto.request.BloquearLoteRequest;
import com.softprimesolutions.inventario.api.dto.request.RegistrarMovimientoRequest;
import com.softprimesolutions.inventario.api.dto.response.LoteResponse;
import com.softprimesolutions.inventario.api.dto.response.MovimientoResponse;
import com.softprimesolutions.inventario.api.dto.response.PaginaResponse;
import com.softprimesolutions.inventario.application.dto.command.BloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.command.DesbloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.query.ListarPosicionesQuery;
import com.softprimesolutions.inventario.application.dto.query.ObtenerLoteQuery;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;

class InventarioControllersTest {

    private static final Jwt JWT = Jwt.withTokenValue("token").header("alg", "none")
            .subject(ACTOR_ID.toString()).claim("tid", TENANT.toString()).build();

    private static void assertConflict(ResponseEntity<?> response) {
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isInstanceOf(ProblemDetail.class);
    }

    @Test
    void listsPositionsOfTheTenantInTheTokenWithTheFilters() {
        var received = new AtomicReference<ListarPosicionesQuery>();
        var controller = new PosicionInventarioController(query -> {
            received.set(query);
            return ok(paginaPosiciones());
        });

        var response = controller.list(JWT, ESTABLECIMIENTO, ALMACEN, SKU, 1, 50);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOfSatisfying(PaginaResponse.class,
                page -> assertThat(page.totalElements()).isEqualTo(1));
        assertThat(received.get()).isEqualTo(new ListarPosicionesQuery(
                TENANT, ESTABLECIMIENTO, ALMACEN, SKU, 1, 50));
    }

    @Test
    void mapsPositionListingFailuresToProblemDetails() {
        var controller = new PosicionInventarioController(query -> conflict());

        assertConflict(controller.list(JWT, null, null, null, 0, 20));
    }

    @Test
    void getsALoteOfTheTenantInTheToken() {
        var received = new AtomicReference<ObtenerLoteQuery>();
        var controller = new LoteController(query -> {
            received.set(query);
            return ok(loteResult());
        }, command -> conflict(), command -> conflict());

        var response = controller.getById(JWT, LOTE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOfSatisfying(LoteResponse.class,
                lote -> assertThat(lote.id()).isEqualTo(LOTE));
        assertThat(received.get()).isEqualTo(new ObtenerLoteQuery(TENANT, LOTE));
    }

    @Test
    void blocksALoteOnBehalfOfTheUserInTheToken() {
        var received = new AtomicReference<BloquearLoteCommand>();
        var controller = new LoteController(query -> conflict(), command -> {
            received.set(command);
            return ok(loteResult());
        }, command -> conflict());

        var response = controller.block(JWT, LOTE, new BloquearLoteRequest("Reclamo"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received.get()).isEqualTo(new BloquearLoteCommand(TENANT, LOTE, "Reclamo", ACTOR_ID));
    }

    @Test
    void unblocksALoteOnBehalfOfTheUserInTheToken() {
        var received = new AtomicReference<DesbloquearLoteCommand>();
        var controller = new LoteController(query -> conflict(), command -> conflict(), command -> {
            received.set(command);
            return ok(loteResult());
        });

        var response = controller.unblock(JWT, LOTE);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(received.get()).isEqualTo(new DesbloquearLoteCommand(TENANT, LOTE, ACTOR_ID));
    }

    @Test
    void mapsLoteFailuresToProblemDetails() {
        var controller = new LoteController(query -> conflict(), command -> conflict(), command -> conflict());

        assertConflict(controller.getById(JWT, LOTE));
        assertConflict(controller.block(JWT, LOTE, new BloquearLoteRequest("x")));
        assertConflict(controller.unblock(JWT, LOTE));
    }

    @Test
    void registersAMovementAndAnswersCreated() {
        var received = new AtomicReference<RegistrarMovimientoCommand>();
        var controller = new MovimientoInventarioController(command -> {
            received.set(command);
            return ok(new MovimientoResult(
                    POSICION, POSICION, LOTE, "AJUSTE_INGRESO", "E", new BigDecimal("5"), BigDecimal.ZERO,
                    new BigDecimal("5"), AHORA));
        });
        var request = new RegistrarMovimientoRequest(
                ALMACEN, SKU, LOTE, null, null, "AJUSTE_INGRESO", new BigDecimal("5"), "Saldo inicial");

        var response = controller.register(JWT, "clave-1", request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isInstanceOfSatisfying(MovimientoResponse.class,
                movimiento -> assertThat(movimiento.stockPosterior()).isEqualByComparingTo("5"));
        assertThat(received.get().tenantId()).isEqualTo(TENANT);
        assertThat(received.get().actorId()).isEqualTo(ACTOR_ID);
        assertThat(received.get().skuId()).isEqualTo(SKU);
        assertThat(received.get().idempotencyKey()).isEqualTo("clave-1");
    }

    @Test
    void mapsMovementFailuresToProblemDetails() {
        var controller = new MovimientoInventarioController(command -> conflict());
        var request = new RegistrarMovimientoRequest(
                ALMACEN, SKU, LOTE, null, null, "AJUSTE_SALIDA", new BigDecimal("5"), "Merma");

        assertConflict(controller.register(JWT, null, request));
    }
}
