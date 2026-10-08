package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR;
import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.AHORA;
import static com.softprimesolutions.compras.ComprasFixtures.EMPRESA;
import static com.softprimesolutions.compras.ComprasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.HOY;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.SKU;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.domainValue;
import static com.softprimesolutions.compras.ComprasFixtures.ordenNueva;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.compras.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.compras.infrastructure.persistence.Rows;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionOperations;

class OrdenCompraJdbcWriteAdapterTest {

    private static final String HEADER = "AND o.es_activo = '1'";
    private static final String LINEAS = "ORDER BY l.numero_linea";
    private static final String INSERTAR_ORDEN = "INSERT INTO sch_abastecimiento.orden_compra\n";
    private static final String INSERTAR_LINEA = "INSERT INTO sch_abastecimiento.orden_compra_linea";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final OrdenCompraJdbcWriteAdapter adapter =
            new OrdenCompraJdbcWriteAdapter(jdbc.client(), TransactionOperations.withoutTransaction());

    @Test
    void findsAnOrderWithItsLinesAndReceivedQuantities() {
        jdbc.rows(HEADER, Rows.ordenEncabezado(true)).rows(LINEAS, Rows.ordenLinea());

        var orden = adapter.findById(TENANT, ORDEN);

        assertThat(orden).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(ORDEN);
            assertThat(found.tenantId()).isEqualTo(TENANT);
            assertThat(found.empresaId()).isEqualTo(EMPRESA);
            assertThat(found.proveedorId()).isEqualTo(PROVEEDOR);
            assertThat(found.establecimientoDestinoId()).isEqualTo(ESTABLECIMIENTO);
            assertThat(found.fechaEmision()).isEqualTo(HOY);
            assertThat(found.estado()).isEqualTo(EstadoOrdenCompra.APROBADA);
            assertThat(found.aprobadoPor()).isEqualTo(ACTOR_ID.toString());
            assertThat(found.aprobadoAt()).isNotNull();
            assertThat(found.condiciones().fechaEntregaEstimada()).isNull();
            assertThat(found.condiciones().moneda()).isEqualTo("PEN");
            assertThat(found.total()).isEqualByComparingTo("64.90");
            assertThat(found.lineas()).singleElement().satisfies(linea -> {
                assertThat(linea.skuId()).isEqualTo(SKU);
                assertThat(linea.cantidadRecibida()).isEqualByComparingTo("4");
                assertThat(linea.toleranciaExcesoPct()).isEqualByComparingTo("10");
            });
        });
        assertThat(jdbc.statementContaining(HEADER).sql()).doesNotContain("FOR UPDATE");
        assertThat(jdbc.statementContaining(HEADER).params())
                .containsEntry("tenantId", TENANT).containsEntry("ordenId", ORDEN);
    }

    @Test
    void anOrderWithoutApprovalHasNoApprover() {
        jdbc.rows(HEADER, Rows.ordenEncabezado(false)).rows(LINEAS, Rows.ordenLinea());

        var orden = adapter.findById(TENANT, ORDEN);

        assertThat(orden).hasValueSatisfying(found -> {
            assertThat(found.aprobadoPor()).isNull();
            assertThat(found.aprobadoAt()).isNull();
        });
    }

    @Test
    void findsNoOrderForAnUnknownId() {
        assertThat(adapter.findById(TENANT, ORDEN)).isEmpty();
        assertThat(jdbc.statements()).hasSize(1);
    }

    @Test
    void locksTheOrderRowWhenItIsGoingToBeUpdated() {
        jdbc.rows(HEADER, Rows.ordenEncabezado(true)).rows(LINEAS, Rows.ordenLinea());

        assertThat(adapter.findByIdParaActualizar(TENANT, ORDEN)).isPresent();

        assertThat(jdbc.statementContaining(HEADER).sql()).contains("FOR UPDATE OF o");
    }

    @Test
    void insertsTheOrderAndEachOfItsLines() {
        var orden = ordenNueva();

        var outcome = adapter.insertar(orden);

        assertThat(outcome).isEqualTo(GuardadoOutcome.GUARDADO);
        assertThat(jdbc.statements()).extracting(JdbcClientStub.Statement::sql).satisfiesExactly(
                sql -> assertThat(sql).contains(INSERTAR_ORDEN),
                sql -> assertThat(sql).contains(INSERTAR_LINEA));
        assertThat(jdbc.statementContaining(INSERTAR_ORDEN).params())
                .containsEntry("ordenId", ORDEN).containsEntry("tenantId", TENANT)
                .containsEntry("establecimientoId", ESTABLECIMIENTO).containsEntry("proveedorId", PROVEEDOR)
                .containsEntry("numero", "OC-2026-000001").containsEntry("fechaEmision", HOY)
                .containsEntry("moneda", "PEN").containsEntry("estado", "BORRADOR")
                .containsEntry("observacion", "Reposicion").containsEntry("createdBy", ACTOR_ID.toString());
        assertThat(jdbc.statementContaining(INSERTAR_LINEA).params())
                .containsEntry("ordenId", ORDEN).containsEntry("numeroLinea", 1).containsEntry("skuId", SKU)
                .containsEntry("descripcion", "Paracetamol 500 mg").containsEntry("unidad", "UND")
                .containsEntry("cantidad", dec("10")).containsEntry("totalLinea", dec("64.90"));
    }

    @Test
    void reportsADuplicateWhenTheDatabaseRejectsTheNumber() {
        jdbc.failsWith(INSERTAR_ORDEN, new DuplicateKeyException("uk_orden_compra_numero"));

        assertThat(adapter.insertar(ordenNueva())).isEqualTo(GuardadoOutcome.DUPLICADO);
    }

    @Test
    void reportsADuplicateWhenTheHeaderOrALineInsertsNoRow() {
        jdbc.updates(INSERTAR_ORDEN, 0);
        assertThat(adapter.insertar(ordenNueva())).isEqualTo(GuardadoOutcome.DUPLICADO);

        var otro = new JdbcClientStub().updates(INSERTAR_LINEA, 0);
        var adaptador = new OrdenCompraJdbcWriteAdapter(otro.client(), TransactionOperations.withoutTransaction());
        assertThat(adaptador.insertar(ordenNueva())).isEqualTo(GuardadoOutcome.DUPLICADO);
    }

    @Test
    void updatesTheStateGuardedByThePreviousState() {
        var aprobada = domainValue(ordenNueva().aprobar(ACTOR, AHORA));

        assertThat(adapter.actualizarEstado(aprobada, EstadoOrdenCompra.BORRADOR)).isTrue();

        assertThat(jdbc.statementContaining("UPDATE sch_abastecimiento.orden_compra").params())
                .containsEntry("estado", "APROBADA").containsEntry("estadoPrevio", "BORRADOR")
                .containsEntry("aprobadoPor", ACTOR_ID.toString()).containsEntry("aprobadoAt", Rows.MOMENTO)
                .containsEntry("ordenId", ORDEN).containsEntry("tenantId", TENANT)
                .containsEntry("updatedBy", ACTOR_ID.toString());
    }

    @Test
    void reportsThatNoRowChangedWhenThePreviousStateNoLongerMatches() {
        jdbc.updates("UPDATE sch_abastecimiento.orden_compra", 0);

        assertThat(adapter.actualizarEstado(ordenNueva(), EstadoOrdenCompra.APROBADA)).isFalse();
    }
}
