package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.proveedor;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import com.softprimesolutions.compras.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.compras.infrastructure.persistence.Rows;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;

class ProveedorJdbcWriteAdapterTest {

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final ProveedorJdbcWriteAdapter adapter = new ProveedorJdbcWriteAdapter(jdbc.client());

    @Test
    void findsAProveedorRestoringItsDataStateAndAudit() {
        jdbc.rows("p.uuid_publico = :proveedorId", Rows.proveedor());

        var proveedor = adapter.findById(TENANT, PROVEEDOR);

        assertThat(proveedor).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(PROVEEDOR);
            assertThat(found.tenantId()).isEqualTo(TENANT);
            assertThat(found.estado()).isEqualTo(EstadoProveedor.ACTIVO);
            assertThat(found.createdBy()).isEqualTo(ACTOR_ID.toString());
            assertThat(found.updatedAt()).isNull();
            var datos = found.datos();
            assertThat(datos.numeroDocumento()).isEqualTo("20100070970");
            assertThat(datos.razonSocial()).isEqualTo("Laboratorios SAC");
            assertThat(datos.diasCreditoDefault()).isEqualTo(30);
            assertThat(datos.esLaboratorio()).isTrue();
            assertThat(datos.esImportador()).isFalse();
            assertThat(datos.email()).isEqualTo("ventas@labs.example");
        });
        assertThat(jdbc.statementContaining("p.uuid_publico = :proveedorId").params())
                .containsEntry("tenantId", TENANT).containsEntry("proveedorId", PROVEEDOR);
    }

    @Test
    void findsNoProveedorForAnUnknownId() {
        assertThat(adapter.findById(TENANT, PROVEEDOR)).isEmpty();
    }

    @Test
    void insertsTheProveedorWithItsDataAndAudit() {
        var outcome = adapter.insertar(proveedor(EstadoProveedor.ACTIVO));

        assertThat(outcome).isEqualTo(GuardadoOutcome.GUARDADO);
        var params = jdbc.statementContaining("INSERT INTO sch_abastecimiento.proveedor").params();
        assertThat(params).containsEntry("proveedorId", PROVEEDOR).containsEntry("tenantId", TENANT)
                .containsEntry("tipoDocumento", "6").containsEntry("numeroDocumento", "20100070970")
                .containsEntry("razonSocial", "Laboratorios SAC").containsEntry("diasCreditoDefault", 0)
                .containsEntry("monedaDefault", "PEN").containsEntry("esDistribuidor", true)
                .containsEntry("estado", "ACTIVO").containsEntry("createdAt", Rows.MOMENTO)
                .containsEntry("createdBy", ACTOR_ID.toString());
    }

    @Test
    void reportsADuplicateWhenTheConflictingDocumentInsertsNoRow() {
        jdbc.updates("INSERT INTO sch_abastecimiento.proveedor", 0);

        assertThat(adapter.insertar(proveedor(EstadoProveedor.ACTIVO))).isEqualTo(GuardadoOutcome.DUPLICADO);
    }

    @Test
    void updatesTheProveedor() {
        var outcome = adapter.actualizar(proveedor(EstadoProveedor.BLOQUEADO));

        assertThat(outcome).isEqualTo(GuardadoOutcome.GUARDADO);
        assertThat(jdbc.statementContaining("UPDATE sch_abastecimiento.proveedor").params())
                .containsEntry("proveedorId", PROVEEDOR).containsEntry("estado", "BLOQUEADO");
    }

    @Test
    void reportsADuplicateWhenTheUpdateCollidesWithAnotherDocument() {
        jdbc.failsWith("UPDATE sch_abastecimiento.proveedor", new DuplicateKeyException("uk_proveedor_documento"));

        assertThat(adapter.actualizar(proveedor(EstadoProveedor.ACTIVO))).isEqualTo(GuardadoOutcome.DUPLICADO);
    }
}
