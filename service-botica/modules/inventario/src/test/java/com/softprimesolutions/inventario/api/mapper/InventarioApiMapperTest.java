package com.softprimesolutions.inventario.api.mapper;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR_ID;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.VENCIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.loteResult;
import static com.softprimesolutions.inventario.InventarioFixtures.paginaPosiciones;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.api.dto.request.BloquearLoteRequest;
import com.softprimesolutions.inventario.api.dto.request.RegistrarMovimientoRequest;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InventarioApiMapperTest {

    @Test
    void buildsTheBlockCommandFromTheTokenAndTheRequest() {
        var command = InventarioApiMapper.toCommand(TENANT, LOTE, ACTOR_ID, new BloquearLoteRequest("Reclamo"));

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.loteId()).isEqualTo(LOTE);
        assertThat(command.motivo()).isEqualTo("Reclamo");
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
    }

    @Test
    void buildsTheMovementCommandFromTheTokenAndTheRequest() {
        var request = new RegistrarMovimientoRequest(
                ALMACEN, SKU, LOTE, "L-001", VENCIMIENTO, "AJUSTE_INGRESO", new BigDecimal("5"), "Saldo inicial");

        var command = InventarioApiMapper.toCommand(TENANT, ACTOR_ID, "clave-1", request);

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.almacenId()).isEqualTo(ALMACEN);
        assertThat(command.skuId()).isEqualTo(SKU);
        assertThat(command.loteId()).isEqualTo(LOTE);
        assertThat(command.numeroLote()).isEqualTo("L-001");
        assertThat(command.fechaVencimiento()).isEqualTo(VENCIMIENTO);
        assertThat(command.tipo()).isEqualTo("AJUSTE_INGRESO");
        assertThat(command.cantidad()).isEqualByComparingTo("5");
        assertThat(command.motivo()).isEqualTo("Saldo inicial");
        assertThat(command.actorId()).isEqualTo(ACTOR_ID);
        assertThat(command.idempotencyKey()).isEqualTo("clave-1");
    }

    @Test
    void mapsALoteResultToItsResponse() {
        var response = InventarioApiMapper.toResponse(loteResult());

        assertThat(response.id()).isEqualTo(LOTE);
        assertThat(response.skuId()).isEqualTo(SKU);
        assertThat(response.numeroLote()).isEqualTo("L-001");
        assertThat(response.fechaVencimiento()).isEqualTo(VENCIMIENTO);
        assertThat(response.estado()).isEqualTo("HABILITADO");
        assertThat(response.motivoEstado()).isNull();
        assertThat(response.bloqueadoAt()).isNull();
        assertThat(response.vendible()).isTrue();
    }

    @Test
    void mapsAMovementResultToItsResponse() {
        var movimientoId = UUID.randomUUID();
        var result = new MovimientoResult(
                movimientoId, POSICION, LOTE, "AJUSTE_SALIDA", "S", new BigDecimal("4"), new BigDecimal("10"),
                new BigDecimal("6"), AHORA);

        var response = InventarioApiMapper.toResponse(result);

        assertThat(response.id()).isEqualTo(movimientoId);
        assertThat(response.posicionId()).isEqualTo(POSICION);
        assertThat(response.loteId()).isEqualTo(LOTE);
        assertThat(response.tipo()).isEqualTo("AJUSTE_SALIDA");
        assertThat(response.naturaleza()).isEqualTo("S");
        assertThat(response.cantidad()).isEqualByComparingTo("4");
        assertThat(response.stockAnterior()).isEqualByComparingTo("10");
        assertThat(response.stockPosterior()).isEqualByComparingTo("6");
        assertThat(response.fechaNegocio()).isEqualTo(AHORA);
    }

    @Test
    void mapsAPageOfPositionsToItsResponse() {
        var page = InventarioApiMapper.toPosicionPage(paginaPosiciones());

        assertThat(page.page()).isZero();
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(1);
        assertThat(page.items()).singleElement().satisfies(posicion -> {
            assertThat(posicion.id()).isEqualTo(POSICION);
            assertThat(posicion.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
            assertThat(posicion.almacenId()).isEqualTo(ALMACEN);
            assertThat(posicion.skuId()).isEqualTo(SKU);
            assertThat(posicion.loteId()).isEqualTo(LOTE);
            assertThat(posicion.numeroLote()).isEqualTo("L-001");
            assertThat(posicion.fechaVencimiento()).isEqualTo(VENCIMIENTO);
            assertThat(posicion.estadoLote()).isEqualTo("HABILITADO");
            assertThat(posicion.estadoInventario()).isEqualTo("DISPONIBLE");
            assertThat(posicion.cantidadFisica()).isEqualByComparingTo("10");
            assertThat(posicion.cantidadReservada()).isEqualByComparingTo("2");
            assertThat(posicion.cantidadDisponible()).isEqualByComparingTo("8");
            assertThat(posicion.vendible()).isTrue();
            assertThat(posicion.version()).isEqualTo(3L);
            assertThat(posicion.ultimoMovimientoAt()).isEqualTo(AHORA);
        });
    }
}
