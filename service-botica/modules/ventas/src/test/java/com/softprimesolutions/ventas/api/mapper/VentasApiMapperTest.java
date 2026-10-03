package com.softprimesolutions.ventas.api.mapper;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turnoResult;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.LineaVentaRequest;
import com.softprimesolutions.ventas.api.dto.request.PagoEfectivoRequest;
import com.softprimesolutions.ventas.api.dto.request.VentaRequest;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import java.util.List;
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

    @Test
    void mapsTheSaleRequestToACommandWithTheKeyTenantAndActor() {
        var request = new VentaRequest(
                TERMINAL, ALMACEN, List.of(new LineaVentaRequest(SKU, dec("5"), dec("2.50"))),
                new PagoEfectivoRequest(dec("20")));

        var command = VentasApiMapper.toCommand(TENANT, ACTOR_ID, "clave-1", request);

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.idempotencyKey()).isEqualTo("clave-1");
        assertThat(command.terminalId()).isEqualTo(TERMINAL);
        assertThat(command.almacenId()).isEqualTo(ALMACEN);
        assertThat(command.montoRecibido()).isEqualTo(dec("20"));
        assertThat(command.lineas()).hasSize(1);
        assertThat(command.lineas().getFirst().skuId()).isEqualTo(SKU);
        assertThat(command.lineas().getFirst().cantidad()).isEqualTo(dec("5"));
        assertThat(command.lineas().getFirst().precioUnitario()).isEqualTo(dec("2.50"));
    }

    @Test
    void mapsASaleResultToItsResponse() {
        var response = VentasApiMapper.toResponse(ventaResult());

        assertThat(response.id()).isEqualTo(VENTA);
        assertThat(response.numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(response.turnoId()).isEqualTo(TURNO);
        assertThat(response.total()).isEqualTo(dec("12.50"));
        assertThat(response.lineas()).hasSize(1);
        assertThat(response.lineas().getFirst().skuId()).isEqualTo(SKU);
        assertThat(response.lineas().getFirst().lotes()).hasSize(1);
        assertThat(response.lineas().getFirst().lotes().getFirst().cantidad()).isEqualTo(dec("5"));
        assertThat(response.pago().medioPago()).isEqualTo("EFECTIVO");
        assertThat(response.pago().vuelto()).isEqualTo(dec("7.50"));
    }

    @Test
    void mapsASalesPageToItsResponse() {
        var pagina = new PaginaResult<>(
                List.of(new VentaResumenResult(VENTA, "POS01-000001", TERMINAL, AHORA, dec("12.50"), "CONFIRMADA")),
                1, 20, 21L);

        var response = VentasApiMapper.toResponse(pagina);

        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(20);
        assertThat(response.totalElements()).isEqualTo(21L);
        assertThat(response.items()).hasSize(1);
        assertThat(response.items().getFirst().id()).isEqualTo(VENTA);
        assertThat(response.items().getFirst().numeroOperacion()).isEqualTo("POS01-000001");
        assertThat(response.items().getFirst().total()).isEqualTo(dec("12.50"));
    }
}
