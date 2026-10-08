package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.compras.ComprasFixtures.ACTOR_ID;
import static com.softprimesolutions.compras.ComprasFixtures.ALMACEN;
import static com.softprimesolutions.compras.ComprasFixtures.LINEA_RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.VENCIMIENTO;
import static com.softprimesolutions.compras.ComprasFixtures.dec;
import static com.softprimesolutions.compras.ComprasFixtures.recepcion;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.compras.infrastructure.persistence.Rows;
import java.util.HashMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionOperations;

class RecepcionJdbcWriteAdapterTest {

    private static final UUID BUSINESS_UUID = UUID.fromString("99999999-9999-4999-8999-999999999999");
    private static final String INSERTAR_RECEPCION = "INSERT INTO sch_abastecimiento.recepcion_compra\n";
    private static final String INSERTAR_LINEA = "INSERT INTO sch_abastecimiento.recepcion_compra_linea";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final RecepcionJdbcWriteAdapter adapter =
            new RecepcionJdbcWriteAdapter(jdbc.client(), TransactionOperations.withoutTransaction());

    @Test
    void findsThePreviousReceptionOfAnIdempotencyKeyWithItsFingerprint() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", RECEPCION);
        row.put("huella_solicitud", "huella-1");
        jdbc.rows("r.business_uuid = :businessUuid", row);

        var existente = adapter.findPorBusinessUuid(TENANT, BUSINESS_UUID);

        assertThat(existente).hasValueSatisfying(found -> {
            assertThat(found.id()).isEqualTo(RECEPCION);
            assertThat(found.huella()).isEqualTo("huella-1");
        });
        assertThat(jdbc.statementContaining("r.business_uuid = :businessUuid").params())
                .containsEntry("tenantId", TENANT).containsEntry("businessUuid", BUSINESS_UUID);
    }

    @Test
    void findsNothingForAnUnknownKey() {
        assertThat(adapter.findPorBusinessUuid(TENANT, BUSINESS_UUID)).isEmpty();
    }

    @Test
    void insertsTheReceptionAndEachOfItsLines() {
        var recepcion = recepcion();

        var outcome = adapter.insertar(recepcion, BUSINESS_UUID, "huella-1");

        assertThat(outcome).isEqualTo(GuardadoOutcome.GUARDADO);
        assertThat(jdbc.statements()).extracting(JdbcClientStub.Statement::sql).satisfiesExactly(
                sql -> assertThat(sql).contains(INSERTAR_RECEPCION),
                sql -> assertThat(sql).contains(INSERTAR_LINEA));
        assertThat(jdbc.statementContaining(INSERTAR_RECEPCION).params())
                .containsEntry("recepcionId", RECEPCION).containsEntry("tenantId", TENANT)
                .containsEntry("ordenId", ORDEN).containsEntry("almacenId", ALMACEN)
                .containsEntry("numero", "REC-2026-000001").containsEntry("documentoTipo", "01")
                .containsEntry("documentoSerie", "F001").containsEntry("guiaRemitente", "T001-45")
                .containsEntry("recibidoPor", ACTOR_ID.toString()).containsEntry("businessUuid", BUSINESS_UUID)
                .containsEntry("huella", "huella-1").containsEntry("fechaRecepcion", Rows.MOMENTO);
        assertThat(jdbc.statementContaining(INSERTAR_LINEA).params())
                .containsEntry("lineaId", LINEA_RECEPCION).containsEntry("recepcionId", RECEPCION)
                .containsEntry("numeroLinea", 1).containsEntry("numeroLineaOrden", 1)
                .containsEntry("numeroLote", "LOTE-1").containsEntry("fechaVencimiento", VENCIMIENTO)
                .containsEntry("recibida", dec("6")).containsEntry("aceptada", dec("5"))
                .containsEntry("rechazada", dec("1")).containsEntry("decision", "ACEPTADO_PARCIAL")
                .containsEntry("motivo", "Envase danado");
    }

    @Test
    void reportsADuplicateWhenTheBusinessUuidIsAlreadyStored() {
        jdbc.failsWith(INSERTAR_RECEPCION, new DuplicateKeyException("uk_recepcion_business_uuid"));

        assertThat(adapter.insertar(recepcion(), BUSINESS_UUID, "h")).isEqualTo(GuardadoOutcome.DUPLICADO);
    }

    @Test
    void reportsADuplicateWhenTheHeaderOrALineInsertsNoRow() {
        jdbc.updates(INSERTAR_RECEPCION, 0);
        assertThat(adapter.insertar(recepcion(), BUSINESS_UUID, "h")).isEqualTo(GuardadoOutcome.DUPLICADO);

        var otro = new JdbcClientStub().updates(INSERTAR_LINEA, 0);
        var adaptador = new RecepcionJdbcWriteAdapter(otro.client(), TransactionOperations.withoutTransaction());
        assertThat(adaptador.insertar(recepcion(), BUSINESS_UUID, "h")).isEqualTo(GuardadoOutcome.DUPLICADO);
    }
}
