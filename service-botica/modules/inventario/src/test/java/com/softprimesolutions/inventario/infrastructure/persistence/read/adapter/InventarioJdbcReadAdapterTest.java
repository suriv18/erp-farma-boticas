package com.softprimesolutions.inventario.infrastructure.persistence.read.adapter;

import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.VENCIMIENTO;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.infrastructure.persistence.JdbcClientStub;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class InventarioJdbcReadAdapterTest {

    private static final OffsetDateTime MOMENTO = AHORA.atOffset(ZoneOffset.UTC);

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final InventarioJdbcReadAdapter adapter = new InventarioJdbcReadAdapter(jdbc.client());

    private static Map<String, Object> posicionRow() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", POSICION);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("almacen_uuid", ALMACEN);
        row.put("sku_uuid", SKU);
        row.put("lote_uuid", LOTE);
        row.put("numero_lote", "L-001");
        row.put("fecha_vencimiento", VENCIMIENTO);
        row.put("estado_lote", "HABILITADO");
        row.put("estado_inventario", "DISPONIBLE");
        row.put("cantidad_fisica", new BigDecimal("10.0000"));
        row.put("cantidad_reservada", new BigDecimal("2.0000"));
        row.put("cantidad_disponible", new BigDecimal("8.0000"));
        row.put("vendible", true);
        row.put("version_lock", 3L);
        row.put("ultimo_movimiento_at", MOMENTO);
        return row;
    }

    private static Map<String, Object> loteRow() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", LOTE);
        row.put("sku_uuid", SKU);
        row.put("numero_lote", "L-001");
        row.put("fecha_vencimiento", VENCIMIENTO);
        row.put("estado_lote", "BLOQUEADO");
        row.put("motivo_estado", "Reclamo");
        row.put("bloqueado_at", MOMENTO);
        row.put("vendible", false);
        return row;
    }

    @Test
    void listsThePositionsOfAPageWithTheTotalCount() {
        jdbc.rows("SELECT p.uuid_publico", posicionRow()).scalar("COUNT(*)", 7L);

        var page = adapter.findPosiciones(TENANT, ESTABLECIMIENTO, ALMACEN, SKU, 2, 10);

        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(10);
        assertThat(page.totalElements()).isEqualTo(7);
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
        var list = jdbc.statementContaining("SELECT p.uuid_publico");
        assertThat(list.params()).containsEntry("tenantId", TENANT).containsEntry("establecimientoId", ESTABLECIMIENTO)
                .containsEntry("almacenId", ALMACEN).containsEntry("skuId", SKU).containsEntry("limit", 10)
                .containsEntry("offset", 20);
        assertThat(jdbc.statementContaining("COUNT(*)").params()).doesNotContainKeys("limit", "offset");
    }

    @Test
    void bindsNullFiltersWhenNoneAreGiven() {
        jdbc.scalar("COUNT(*)", 0L);

        var page = adapter.findPosiciones(TENANT, null, null, null, 0, 20);

        assertThat(page.items()).isEmpty();
        assertThat(jdbc.statementContaining("COUNT(*)").params())
                .containsEntry("establecimientoId", null).containsEntry("almacenId", null)
                .containsEntry("skuId", null);
    }

    @Test
    void getsALoteByIdWithItsBlockData() {
        jdbc.rows("FROM sch_inventario.lote l", loteRow());

        var lote = adapter.findLoteById(TENANT, LOTE);

        assertThat(lote).hasValueSatisfying(result -> {
            assertThat(result.id()).isEqualTo(LOTE);
            assertThat(result.skuId()).isEqualTo(SKU);
            assertThat(result.numeroLote()).isEqualTo("L-001");
            assertThat(result.fechaVencimiento()).isEqualTo(VENCIMIENTO);
            assertThat(result.estado()).isEqualTo("BLOQUEADO");
            assertThat(result.motivoEstado()).isEqualTo("Reclamo");
            assertThat(result.bloqueadoAt()).isEqualTo(AHORA);
            assertThat(result.vendible()).isFalse();
        });
        assertThat(jdbc.statementContaining("FROM sch_inventario.lote l").params())
                .containsEntry("tenantId", TENANT).containsEntry("loteId", LOTE);
    }

    @Test
    void returnsNothingWhenTheLoteDoesNotBelongToTheTenant() {
        assertThat(adapter.findLoteById(TENANT, LOTE)).isEmpty();
    }
}
