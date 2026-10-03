package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.LINEA;
import static com.softprimesolutions.ventas.VentasFixtures.LOTE;
import static com.softprimesolutions.ventas.VentasFixtures.SKU;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.venta;
import static com.softprimesolutions.ventas.infrastructure.persistence.Rows.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import java.util.HashMap;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.transaction.support.TransactionOperations;

class VentaJdbcWriteAdapterTest {

    private static final String INSERT_VENTA = "INSERT INTO sch_venta.venta\n";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final VentaJdbcWriteAdapter adapter =
            new VentaJdbcWriteAdapter(jdbc.client(), TransactionOperations.withoutTransaction());

    @Test
    void findsASaleByItsIdempotencyKeyWithItsRequestFingerprint() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", VENTA);
        row.put("huella_solicitud", "huella-1");
        jdbc.rows("v.idempotency_key = :idempotencyKey", row);

        var existente = adapter.findPorIdempotencia(TENANT, "clave-1").orElseThrow();

        assertThat(existente.id()).isEqualTo(VENTA);
        assertThat(existente.huella()).isEqualTo("huella-1");
        assertThat(jdbc.statementContaining("v.idempotency_key = :idempotencyKey").params())
                .containsEntry("tenantId", TENANT).containsEntry("idempotencyKey", "clave-1");
    }

    @Test
    void aMissingKeyIsEmpty() {
        assertThat(adapter.findPorIdempotencia(TENANT, "clave-1")).isEmpty();
    }

    @Test
    void insertsTheSaleItsLinesAndTheCashPaymentEnsuringTheCashMethod() {
        var outcome = adapter.insertar(venta(), "clave-1", "huella-1");

        assertThat(outcome).isEqualTo(GuardadoOutcome.GUARDADO);
        var cabecera = jdbc.statementContaining(INSERT_VENTA).params();
        assertThat(cabecera)
                .containsEntry("ventaId", VENTA).containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO)
                .containsEntry("vendedorId", ACTOR_ID).containsEntry("numero", "POS01-000001")
                .containsEntry("idempotencyKey", "clave-1").containsEntry("huella", "huella-1")
                .containsEntry("fecha", MOMENTO).containsEntry("subtotal", dec("12.50"))
                .containsEntry("total", dec("12.50")).containsEntry("actor", ACTOR.codigo());
        var linea = jdbc.statementContaining("INSERT INTO sch_venta.venta_linea\n").params();
        assertThat(linea)
                .containsEntry("lineaId", LINEA).containsEntry("ventaId", VENTA).containsEntry("numeroLinea", 1)
                .containsEntry("skuId", SKU).containsEntry("descripcion", "Paracetamol 500 mg")
                .containsEntry("unidad", "UND").containsEntry("esFraccion", false)
                .containsEntry("cantidad", dec("5")).containsEntry("precio", dec("2.50"))
                .containsEntry("totalLinea", dec("12.50"));
        assertThat(jdbc.statementContaining("INSERT INTO sch_venta.medio_pago").params())
                .containsEntry("tenantId", TENANT).containsEntry("actor", ACTOR.codigo());
        var pago = jdbc.statementContaining("INSERT INTO sch_venta.pago_venta").params();
        assertThat(pago)
                .containsEntry("ventaId", VENTA).containsEntry("monto", dec("12.50"))
                .containsEntry("montoRecibido", dec("20.00")).containsEntry("vuelto", dec("7.50"))
                .containsEntry("fecha", MOMENTO);
    }

    @Test
    void aUniqueViolationOrASaleThatInsertsNoRowIsReportedAsDuplicate() {
        jdbc.failsWith(INSERT_VENTA, new DuplicateKeyException("uk_venta_idempotency"));
        assertThat(adapter.insertar(venta(), "clave-1", "huella-1")).isEqualTo(GuardadoOutcome.DUPLICADO);

        var sinFilas = new JdbcClientStub().updates(INSERT_VENTA, 0);
        var otro = new VentaJdbcWriteAdapter(sinFilas.client(), TransactionOperations.withoutTransaction());
        assertThat(otro.insertar(venta(), "clave-1", "huella-1")).isEqualTo(GuardadoOutcome.DUPLICADO);
    }

    @Test
    void recordsEachConsumedLoteOfALine() {
        adapter.registrarLotes(TENANT, LINEA, List.of(new LoteConsumo(LOTE, dec("5"))));

        assertThat(jdbc.statementContaining("INSERT INTO sch_venta.venta_linea_lote").params())
                .containsEntry("lineaId", LINEA).containsEntry("loteId", LOTE).containsEntry("cantidad", dec("5"))
                .containsEntry("tenantId", TENANT);
    }

    @Test
    void failsLoudlyWhenALoteRowCannotBeInserted() {
        var sinFilas = new JdbcClientStub().updates("INSERT INTO sch_venta.venta_linea_lote", 0);
        var otro = new VentaJdbcWriteAdapter(sinFilas.client(), TransactionOperations.withoutTransaction());

        assertThatThrownBy(() -> otro.registrarLotes(TENANT, LINEA, List.of(new LoteConsumo(LOTE, dec("5")))))
                .isInstanceOf(RuntimeException.class);
    }
}
