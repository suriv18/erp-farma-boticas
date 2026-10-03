package com.softprimesolutions.inventario.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.inventario.InventarioFixtures.ACTOR;
import static com.softprimesolutions.inventario.InventarioFixtures.AHORA;
import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.POSICION;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.VENCIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.loteHabilitado;
import static com.softprimesolutions.inventario.InventarioFixtures.posicion;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort.RegistroOutcome;
import com.softprimesolutions.inventario.application.port.out.RegistroMovimiento;
import com.softprimesolutions.inventario.domain.model.EstadoLote;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import com.softprimesolutions.inventario.infrastructure.persistence.JdbcClientStub;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.softprimesolutions.inventario.InventarioFixtures.HOY;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionOperations;

class InventarioJdbcWriteAdapterTest {

    private static final OffsetDateTime MOMENTO = AHORA.atOffset(ZoneOffset.UTC);
    private static final UUID BUSINESS_UUID = UUID.fromString("99999999-9999-4999-8999-999999999999");

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final InventarioJdbcWriteAdapter adapter =
            new InventarioJdbcWriteAdapter(jdbc.client(), TransactionOperations.withoutTransaction());

    private static Map<String, Object> loteRow() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", LOTE);
        row.put("sku_uuid", SKU);
        row.put("numero_lote", "L-001");
        row.put("fecha_vencimiento", VENCIMIENTO);
        row.put("estado_lote", "BLOQUEADO");
        row.put("motivo_estado", "Reclamo");
        row.put("bloqueado_at", MOMENTO);
        row.put("bloqueado_por", "777777777777477");
        row.put("created_at", MOMENTO);
        row.put("updated_at", MOMENTO);
        row.put("updated_by", "777777777777477");
        return row;
    }

    private static Map<String, Object> posicionRow() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", POSICION);
        row.put("lote_uuid", LOTE);
        row.put("almacen_uuid", ALMACEN);
        row.put("sku_uuid", SKU);
        row.put("cantidad_fisica", new BigDecimal("10.0000"));
        row.put("cantidad_reservada", new BigDecimal("2.0000"));
        row.put("version_lock", 5L);
        return row;
    }

    private static RegistroMovimiento registro(
            TipoMovimiento tipo, boolean loteNuevo, boolean posicionNueva, String fisicaPosterior) {
        return new RegistroMovimiento(
                UUID.fromString("88888888-8888-4888-8888-888888888888"), new TenantId(TENANT), ALMACEN,
                SKU, loteHabilitado(), loteNuevo, posicion(fisicaPosterior, "0", 5), posicionNueva, tipo,
                new BigDecimal("4"), new BigDecimal("10"), new BigDecimal(fisicaPosterior), "Merma por rotura",
                ACTOR, AHORA, BUSINESS_UUID, "huella-1");
    }

    @Test
    void findsALoteByIdRestoringItsStateAndBlockData() {
        jdbc.rows("l.uuid_publico = :loteId", loteRow());

        var lote = adapter.findLote(TENANT, LOTE);

        assertThat(lote).hasValueSatisfying(found -> {
            assertThat(found.id().value()).isEqualTo(LOTE);
            assertThat(found.tenantId().value()).isEqualTo(TENANT);
            assertThat(found.skuId()).isEqualTo(SKU);
            assertThat(found.numeroLote()).isEqualTo("L-001");
            assertThat(found.fechaVencimiento()).isEqualTo(VENCIMIENTO);
            assertThat(found.estado()).isEqualTo(EstadoLote.BLOQUEADO);
            assertThat(found.motivoEstado()).isEqualTo("Reclamo");
            assertThat(found.bloqueadoAt()).isEqualTo(AHORA);
            assertThat(found.bloqueadoPor()).isEqualTo("777777777777477");
            assertThat(found.createdAt()).isEqualTo(AHORA);
            assertThat(found.updatedAt()).isEqualTo(AHORA);
            assertThat(found.updatedBy()).isEqualTo("777777777777477");
        });
        assertThat(jdbc.statementContaining("l.uuid_publico = :loteId").params())
                .containsEntry("tenantId", TENANT).containsEntry("loteId", LOTE);
    }

    @Test
    void findsNoLoteForAnUnknownId() {
        assertThat(adapter.findLote(TENANT, LOTE)).isEmpty();
    }

    @Test
    void findsALoteByItsNaturalKey() {
        jdbc.rows("l.numero_lote = :numeroLote", loteRow());

        var lote = adapter.findLotePorClave(TENANT, SKU, "L-001", VENCIMIENTO);

        assertThat(lote).isPresent();
        assertThat(jdbc.statementContaining("l.numero_lote = :numeroLote").params())
                .containsEntry("skuId", SKU).containsEntry("numeroLote", "L-001")
                .containsEntry("fechaVencimiento", VENCIMIENTO);
    }

    @Test
    void findsTheAvailablePositionOfALoteInAWarehouse() {
        jdbc.rows("p.estado_inventario = 'DISPONIBLE'", posicionRow());

        var posicion = adapter.findPosicion(TENANT, ALMACEN, LOTE);

        assertThat(posicion).hasValueSatisfying(found -> {
            assertThat(found.id().value()).isEqualTo(POSICION);
            assertThat(found.loteId()).isEqualTo(LOTE);
            assertThat(found.almacenId()).isEqualTo(ALMACEN);
            assertThat(found.skuId()).isEqualTo(SKU);
            assertThat(found.cantidadFisica()).isEqualByComparingTo("10");
            assertThat(found.cantidadReservada()).isEqualByComparingTo("2");
            assertThat(found.version()).isEqualTo(5L);
        });
        assertThat(jdbc.statementContaining("p.estado_inventario = 'DISPONIBLE'").params())
                .containsEntry("almacenId", ALMACEN).containsEntry("loteId", LOTE);
    }

    @Test
    void findsThePreviousMovementOfAnIdempotencyKeyWithItsFingerprint() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", UUID.fromString("88888888-8888-4888-8888-888888888888"));
        row.put("posicion_uuid", POSICION);
        row.put("lote_uuid", LOTE);
        row.put("tipo_movimiento", "AJUSTE_SALIDA");
        row.put("naturaleza", "S");
        row.put("cantidad", new BigDecimal("4"));
        row.put("stock_anterior", new BigDecimal("10"));
        row.put("stock_posterior", new BigDecimal("6"));
        row.put("fecha_negocio", MOMENTO);
        row.put("huella", "huella-1");
        jdbc.rows("m.business_uuid = :businessUuid", row);

        var previo = adapter.findMovimientoPorBusinessUuid(TENANT, BUSINESS_UUID);

        assertThat(previo).hasValueSatisfying(found -> {
            assertThat(found.huella()).isEqualTo("huella-1");
            assertThat(found.resultado().id()).isEqualTo(UUID.fromString("88888888-8888-4888-8888-888888888888"));
            assertThat(found.resultado().posicionId()).isEqualTo(POSICION);
            assertThat(found.resultado().loteId()).isEqualTo(LOTE);
            assertThat(found.resultado().tipo()).isEqualTo("AJUSTE_SALIDA");
            assertThat(found.resultado().naturaleza()).isEqualTo("S");
            assertThat(found.resultado().cantidad()).isEqualByComparingTo("4");
            assertThat(found.resultado().stockAnterior()).isEqualByComparingTo("10");
            assertThat(found.resultado().stockPosterior()).isEqualByComparingTo("6");
            assertThat(found.resultado().fechaNegocio()).isEqualTo(AHORA);
        });
        assertThat(jdbc.statementContaining("m.business_uuid = :businessUuid").params())
                .containsEntry("tenantId", TENANT).containsEntry("businessUuid", BUSINESS_UUID);
    }

    @Test
    void findsNoPreviousMovementForAnUnknownKey() {
        assertThat(adapter.findMovimientoPorBusinessUuid(TENANT, BUSINESS_UUID)).isEmpty();
    }

    @Test
    void findsNoPositionWhenNothingWasStoredYet() {
        assertThat(adapter.findPosicion(TENANT, ALMACEN, LOTE)).isEmpty();
    }

    @Test
    void updatesTheStateOfALoteGuardedByItsPreviousState() {
        var bloqueado = loteHabilitado().bloquear("Reclamo", ACTOR, AHORA)
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(adapter.actualizarEstado(bloqueado, EstadoLote.HABILITADO)).isTrue();

        assertThat(jdbc.statementContaining("UPDATE sch_inventario.lote").params())
                .containsEntry("estado", "BLOQUEADO").containsEntry("motivo", "Reclamo")
                .containsEntry("bloqueadoAt", MOMENTO).containsEntry("bloqueadoPor", ACTOR.codigo())
                .containsEntry("updatedAt", MOMENTO).containsEntry("updatedBy", ACTOR.codigo())
                .containsEntry("loteId", LOTE).containsEntry("estadoPrevio", "HABILITADO")
                .containsEntry("tenantId", TENANT);
    }

    @Test
    void reportsThatNoRowChangedWhenThePreviousStateNoLongerMatches() {
        jdbc.updates("UPDATE sch_inventario.lote", 0);

        assertThat(adapter.actualizarEstado(loteHabilitado(), EstadoLote.BLOQUEADO)).isFalse();
    }

    @Test
    void insertsTheLoteThePositionAndTheKardexEntryOfAFirstIngreso() {
        var outcome = adapter.registrar(registro(TipoMovimiento.AJUSTE_INGRESO, true, true, "14"));

        assertThat(outcome).isEqualTo(RegistroOutcome.REGISTRADO);
        assertThat(jdbc.statements()).extracting(JdbcClientStub.Statement::sql).satisfiesExactly(
                sql -> assertThat(sql).contains("INSERT INTO sch_inventario.lote"),
                sql -> assertThat(sql).contains("INSERT INTO sch_inventario.posicion_inventario"),
                sql -> assertThat(sql).contains("INSERT INTO sch_inventario.movimiento_inventario"));
        assertThat(jdbc.statementContaining("INSERT INTO sch_inventario.lote").params())
                .containsEntry("loteId", LOTE).containsEntry("numeroLote", "L-001")
                .containsEntry("fechaVencimiento", VENCIMIENTO).containsEntry("fecha", MOMENTO)
                .containsEntry("actor", ACTOR.codigo()).containsEntry("skuId", SKU)
                .containsEntry("tenantId", TENANT);
        assertThat(jdbc.statementContaining("INSERT INTO sch_inventario.posicion_inventario").params())
                .containsEntry("posicionId", POSICION).containsEntry("cantidadFisica", new BigDecimal("14"))
                .containsEntry("almacenId", ALMACEN).containsEntry("loteId", LOTE);
    }

    @Test
    void updatesTheExistingPositionGuardedByItsVersionAndWritesTheKardexEntry() {
        var outcome = adapter.registrar(registro(TipoMovimiento.AJUSTE_SALIDA, false, false, "6"));

        assertThat(outcome).isEqualTo(RegistroOutcome.REGISTRADO);
        assertThat(jdbc.statements()).extracting(JdbcClientStub.Statement::sql).satisfiesExactly(
                sql -> assertThat(sql).contains("UPDATE sch_inventario.posicion_inventario"),
                sql -> assertThat(sql).contains("INSERT INTO sch_inventario.movimiento_inventario"));
        assertThat(jdbc.statementContaining("UPDATE sch_inventario.posicion_inventario").params())
                .containsEntry("cantidadFisica", new BigDecimal("6")).containsEntry("version", 5L)
                .containsEntry("posicionId", POSICION).containsEntry("fecha", MOMENTO);
        var movimiento = jdbc.statementContaining("INSERT INTO sch_inventario.movimiento_inventario").params();
        assertThat(movimiento).containsEntry("tipo", "AJUSTE_SALIDA").containsEntry("naturaleza", "S")
                .containsEntry("ingreso", false).containsEntry("cantidad", new BigDecimal("4"))
                .containsEntry("stockAnterior", new BigDecimal("10"))
                .containsEntry("stockPosterior", new BigDecimal("6")).containsEntry("motivo", "Merma por rotura")
                .containsEntry("businessUuid", BUSINESS_UUID).containsEntry("actor", ACTOR.id().toString())
                .containsEntry("metadata", "{\"huellaSolicitud\":\"huella-1\"}")
                .containsEntry("posicionId", POSICION);
    }

    @Test
    void flagsAnIngresoInTheKardexEntry() {
        adapter.registrar(registro(TipoMovimiento.AJUSTE_INGRESO, false, false, "14"));

        var movimiento = jdbc.statementContaining("INSERT INTO sch_inventario.movimiento_inventario").params();
        assertThat(movimiento).containsEntry("naturaleza", "E").containsEntry("ingreso", true);
    }

    @Test
    void recordsThePurchaseOriginInTheLoteAndTheKardexEntry() {
        var origen = new DocumentoOrigen(
                "RECEPCION_COMPRA", UUID.fromString("aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa"),
                UUID.fromString("bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb"),
                UUID.fromString("cccccccc-cccc-4ccc-8ccc-cccccccccccc"));
        var base = registro(TipoMovimiento.INGRESO_COMPRA, true, true, "14");
        var conOrigen = new RegistroMovimiento(
                base.movimientoId(), base.tenantId(), base.almacenId(), base.skuId(), base.lote(), true,
                base.posicion(), true, base.tipo(), base.cantidad(), base.stockAnterior(), base.stockPosterior(),
                base.motivo(), base.actor(), base.fechaNegocio(), base.businessUuid(), base.huella(), origen);

        assertThat(adapter.registrar(conOrigen)).isEqualTo(RegistroOutcome.REGISTRADO);

        assertThat(jdbc.statementContaining("INSERT INTO sch_inventario.lote").params())
                .containsEntry("proveedorId", origen.proveedorId())
                .containsEntry("origenLineaId", origen.lineaId());
        assertThat(jdbc.statementContaining("INSERT INTO sch_inventario.movimiento_inventario").params())
                .containsEntry("tipo", "INGRESO_COMPRA").containsEntry("tipoOperacionSunat", "02")
                .containsEntry("documentoTipo", "RECEPCION_COMPRA")
                .containsEntry("documentoUuid", origen.documentoId());
    }

    @Test
    void anAdjustmentIsRecordedAsAManualDocumentWithoutOrigin() {
        adapter.registrar(registro(TipoMovimiento.AJUSTE_INGRESO, true, true, "14"));

        assertThat(jdbc.statementContaining("INSERT INTO sch_inventario.movimiento_inventario").params())
                .containsEntry("tipoOperacionSunat", "99").containsEntry("documentoTipo", "AJUSTE_MANUAL")
                .containsEntry("documentoUuid", null);
        assertThat(jdbc.statementContaining("INSERT INTO sch_inventario.lote").params())
                .containsEntry("proveedorId", null).containsEntry("origenLineaId", null);
    }

    @Test
    void reportsAConcurrentModificationWhenTheVersionGuardTouchesNoRow() {
        jdbc.updates("UPDATE sch_inventario.posicion_inventario", 0);

        var outcome = adapter.registrar(registro(TipoMovimiento.AJUSTE_SALIDA, false, false, "6"));

        assertThat(outcome).isEqualTo(RegistroOutcome.MODIFICACION_CONCURRENTE);
        assertThat(jdbc.statements()).hasSize(1);
    }

    @Test
    void reportsAConcurrentModificationWhenTheDatabaseRejectsTheRowAsDuplicated() {
        jdbc.failsWith("INSERT INTO sch_inventario.posicion_inventario", new DuplicateKeyException("uk_posicion"));

        var outcome = adapter.registrar(registro(TipoMovimiento.AJUSTE_INGRESO, true, true, "14"));

        assertThat(outcome).isEqualTo(RegistroOutcome.MODIFICACION_CONCURRENTE);
    }

    @Test
    void listsTheSellablePositionsOfASkuInFefoOrderWithItsFilters() {
        var segunda = posicionRow();
        segunda.put("uuid_publico", UUID.fromString("abababab-abab-4bab-8bab-abababababab"));
        segunda.put("cantidad_fisica", new BigDecimal("3.0000"));
        segunda.put("cantidad_reservada", new BigDecimal("0.0000"));
        jdbc.rows("ORDER BY l.fecha_vencimiento", List.of(posicionRow(), segunda));

        var posiciones = adapter.findPosicionesVendiblesFefo(TENANT, ALMACEN, SKU, HOY);

        assertThat(posiciones).hasSize(2);
        assertThat(posiciones.get(0).id().value()).isEqualTo(POSICION);
        assertThat(posiciones.get(0).disponible()).isEqualByComparingTo("8");
        assertThat(posiciones.get(1).disponible()).isEqualByComparingTo("3");
        var statement = jdbc.statementContaining("ORDER BY l.fecha_vencimiento");
        assertThat(statement.params())
                .containsEntry("tenantId", TENANT).containsEntry("almacenId", ALMACEN)
                .containsEntry("skuId", SKU).containsEntry("hoy", HOY);
        assertThat(statement.sql())
                .contains("l.estado_lote = 'HABILITADO'")
                .contains("l.es_activo = '1'")
                .contains("l.fecha_vencimiento >= :hoy")
                .contains("p.cantidad_fisica > p.cantidad_reservada")
                .contains("ORDER BY l.fecha_vencimiento, l.numero_lote")
                .contains("FOR UPDATE OF p");
    }
}
