package com.softprimesolutions.compras.infrastructure.persistence.read.adapter;

import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.HOY;
import static com.softprimesolutions.compras.ComprasFixtures.LOTE;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.compras.infrastructure.persistence.Rows;
import java.math.BigDecimal;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class ComprasJdbcReadAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final ComprasJdbcReadAdapter adapter = new ComprasJdbcReadAdapter(jdbc.client());

    @Test
    void findsAProveedorAsItsResult() {
        jdbc.rows("p.uuid_publico = :proveedorId", Rows.proveedor());

        var proveedor = adapter.findProveedor(TENANT, PROVEEDOR);

        assertThat(proveedor).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(PROVEEDOR);
            assertThat(found.razonSocial()).isEqualTo("Laboratorios SAC");
            assertThat(found.estado()).isEqualTo("ACTIVO");
        });
    }

    @Test
    void findsNoProveedorForAnUnknownId() {
        assertThat(adapter.findProveedor(TENANT, PROVEEDOR)).isEmpty();
    }

    @Test
    void listsProveedoresWithTheirFiltersAndTotal() {
        jdbc.rows("LIMIT :limit OFFSET :offset", Rows.proveedor()).scalar("SELECT COUNT(*) FROM (", 7L);

        var pagina = adapter.findProveedores(TENANT, "ACTIVO", "lab", 2, 5);

        assertThat(pagina.items()).singleElement().satisfies(item -> assertThat(item.id()).isEqualTo(PROVEEDOR));
        assertThat(pagina.page()).isEqualTo(2);
        assertThat(pagina.size()).isEqualTo(5);
        assertThat(pagina.totalElements()).isEqualTo(7);
        assertThat(jdbc.statementContaining("LIMIT :limit OFFSET :offset").params())
                .containsEntry("tenantId", TENANT).containsEntry("estado", "ACTIVO")
                .containsEntry("texto", "lab").containsEntry("limit", 5).containsEntry("offset", 10);
        assertThat(jdbc.statementContaining("SELECT COUNT(*) FROM (").params())
                .containsEntry("estado", "ACTIVO").containsEntry("texto", "lab");
    }

    @Test
    void findsAnOrderAsItsResultWithTheOutstandingQuantity() {
        jdbc.rows("AND o.es_activo = '1'", Rows.ordenEncabezado(true)).rows("ORDER BY l.numero_linea", Rows.ordenLinea());

        var orden = adapter.findOrden(TENANT, ORDEN);

        assertThat(orden).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(ORDEN);
            assertThat(found.estado()).isEqualTo("APROBADA");
            assertThat(found.lineas()).singleElement().satisfies(linea -> {
                assertThat(linea.cantidadRecibida()).isEqualByComparingTo("4");
                assertThat(linea.cantidadPendiente()).isEqualByComparingTo("6");
            });
        });
        assertThat(jdbc.statements()).allSatisfy(statement -> assertThat(statement.sql()).doesNotContain("FOR UPDATE"));
    }

    @Test
    void findsNoOrderForAnUnknownId() {
        assertThat(adapter.findOrden(TENANT, ORDEN)).isEmpty();
    }

    @Test
    void listsOrdersWithTheirFiltersAndTotal() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", ORDEN);
        row.put("numero", "OC-2026-000001");
        row.put("proveedor_uuid", PROVEEDOR);
        row.put("razon_social", "Laboratorios SAC");
        row.put("establecimiento_uuid", ESTABLECIMIENTO);
        row.put("fecha_emision", HOY);
        row.put("moneda", "PEN");
        row.put("total", new BigDecimal("64.90"));
        row.put("estado", "EMITIDA");
        jdbc.rows("ORDER BY o.fecha_emision DESC", row).scalar("SELECT COUNT(*) ", 3L);

        var pagina = adapter.findOrdenes(TENANT, PROVEEDOR, "EMITIDA", 0, 20);

        assertThat(pagina.items()).singleElement().satisfies(item -> {
            assertThat(item.id()).isEqualTo(ORDEN);
            assertThat(item.proveedorRazonSocial()).isEqualTo("Laboratorios SAC");
            assertThat(item.estado()).isEqualTo("EMITIDA");
            assertThat(item.fechaEntregaEstimada()).isNull();
        });
        assertThat(pagina.totalElements()).isEqualTo(3);
        assertThat(jdbc.statementContaining("ORDER BY o.fecha_emision DESC").params())
                .containsEntry("proveedorId", PROVEEDOR).containsEntry("estado", "EMITIDA")
                .containsEntry("limit", 20).containsEntry("offset", 0);
    }

    @Test
    void findsAReceptionWithItsLinesAndTheLoteTheyCreated() {
        jdbc.rows("AND r.es_activo = '1'", Rows.recepcion()).rows("ORDER BY rl.numero_linea", Rows.recepcionLinea());

        var recepcion = adapter.findRecepcion(TENANT, RECEPCION);

        assertThat(recepcion).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(RECEPCION);
            assertThat(found.ordenCompraId()).isEqualTo(ORDEN);
            assertThat(found.almacenId()).isEqualTo(ALMACEN);
            assertThat(found.estado()).isEqualTo("CONFIRMADA");
            assertThat(found.temperaturaRecepcionC()).isEqualByComparingTo("22.5");
            assertThat(found.humedadRelativaPct()).isNull();
            assertThat(found.lineas()).singleElement().satisfies(linea -> {
                assertThat(linea.numeroLineaOrden()).isEqualTo(1);
                assertThat(linea.cantidadAceptada()).isEqualByComparingTo("5");
                assertThat(linea.decisionCalidad()).isEqualTo("ACEPTADO_PARCIAL");
                assertThat(linea.loteId()).isEqualTo(LOTE);
                assertThat(linea.fechaFabricacion()).isNull();
            });
        });
    }

    @Test
    void findsNoReceptionForAnUnknownId() {
        assertThat(adapter.findRecepcion(TENANT, RECEPCION)).isEmpty();
        assertThat(jdbc.statements()).hasSize(1);
    }
}
