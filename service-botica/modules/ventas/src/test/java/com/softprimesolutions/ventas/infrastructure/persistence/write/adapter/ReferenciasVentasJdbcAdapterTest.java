package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import java.util.List;
import static com.softprimesolutions.ventas.VentasFixtures.OTRO_SKU;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.ALMACEN;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ReferenciasVentasJdbcAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final ReferenciasVentasJdbcAdapter adapter = new ReferenciasVentasJdbcAdapter(jdbc.client());

    @Test
    void findsATerminalWithItsEstablishmentAndOperability() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", TERMINAL);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("codigo", "POS01");
        row.put("operable", true);
        jdbc.rows("FROM sch_organizacion.terminal_pos tp", row);

        var terminal = adapter.terminal(TENANT, TERMINAL).orElseThrow();

        assertThat(terminal.id()).isEqualTo(TERMINAL);
        assertThat(terminal.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(terminal.codigo()).isEqualTo("POS01");
        assertThat(terminal.operable()).isTrue();
        assertThat(jdbc.statementContaining("FROM sch_organizacion.terminal_pos tp").params())
                .containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
    }

    @Test
    void anUnknownTerminalIsEmpty() {
        assertThat(adapter.terminal(TENANT, TERMINAL)).isEmpty();
    }

    @Test
    void findsAWarehouseWithItsEstablishmentAndOperability() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", ALMACEN);
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("operable", true);
        jdbc.rows("FROM sch_organizacion.almacen a", row);

        var almacen = adapter.almacen(TENANT, ALMACEN).orElseThrow();

        assertThat(almacen.id()).isEqualTo(ALMACEN);
        assertThat(almacen.establecimientoId()).isEqualTo(ESTABLECIMIENTO);
        assertThat(almacen.operable()).isTrue();
        var statement = jdbc.statementContaining("FROM sch_organizacion.almacen a");
        assertThat(statement.sql()).contains("a.permite_lotes AND a.permite_venta");
        assertThat(statement.params()).containsEntry("tenantId", TENANT).containsEntry("almacenId", ALMACEN);
        assertThat(new ReferenciasVentasJdbcAdapter(new JdbcClientStub().client()).almacen(TENANT, ALMACEN)).isEmpty();
    }

    @Test
    void findsTheSellableDataOfTheSkusIndexedById() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", SKU);
        row.put("descripcion_comercial", "Paracetamol 500 mg");
        row.put("unidad_venta_codigo", "UND");
        row.put("permite_venta_fraccion", true);
        row.put("operable", true);
        jdbc.rows("FROM sch_catalogo.sku_comercial k", row);

        var skus = adapter.skus(TENANT, List.of(SKU, OTRO_SKU));

        assertThat(skus).containsOnlyKeys(SKU);
        var sku = skus.get(SKU);
        assertThat(sku.descripcion()).isEqualTo("Paracetamol 500 mg");
        assertThat(sku.unidadVentaCodigo()).isEqualTo("UND");
        assertThat(sku.permiteFraccion()).isTrue();
        assertThat(sku.operable()).isTrue();
        assertThat(jdbc.statementContaining("FROM sch_catalogo.sku_comercial k").params())
                .containsEntry("tenantId", TENANT).containsEntry("skuIds", List.of(SKU, OTRO_SKU));
    }

    @Test
    void noSkusMeansNoQuery() {
        assertThat(adapter.skus(TENANT, List.of())).isEmpty();
        assertThat(jdbc.statements()).isEmpty();
    }
}
