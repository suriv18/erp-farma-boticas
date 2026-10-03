package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.infrastructure.persistence.Rows.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.EstadoVenta;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import java.util.HashMap;
import org.junit.jupiter.api.Test;

class AnulacionJdbcWriteAdapterTest {

    private static final String BLOQUEAR = "FOR UPDATE OF v FOR SHARE OF tc";
    private static final String MARCAR_VENTA = "UPDATE sch_venta.venta";
    private static final String REVERSAR_PAGO = "UPDATE sch_venta.pago_venta";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final AnulacionJdbcWriteAdapter adapter = new AnulacionJdbcWriteAdapter(jdbc.client());

    @Test
    void locksTheSaleForUpdateAndItsTurnoForShareInOneQuery() {
        var row = new HashMap<String, Object>();
        row.put("uuid_publico", VENTA);
        row.put("turno_uuid", TURNO);
        row.put("venta_estado", "CONFIRMADA");
        row.put("turno_estado", "ABIERTO");
        jdbc.rows(BLOQUEAR, row);

        var venta = adapter.bloquearVenta(TENANT, VENTA).orElseThrow();

        assertThat(venta.id()).isEqualTo(VENTA);
        assertThat(venta.turnoId()).isEqualTo(TURNO);
        assertThat(venta.estado()).isEqualTo(EstadoVenta.CONFIRMADA);
        assertThat(venta.estadoTurno()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(jdbc.statementContaining(BLOQUEAR).params())
                .containsEntry("tenantId", TENANT).containsEntry("ventaId", VENTA);
    }

    @Test
    void aMissingSaleIsEmpty() {
        assertThat(adapter.bloquearVenta(TENANT, VENTA)).isEmpty();
    }

    @Test
    void marksTheSaleAnnulledAndReversesItsConfirmedPayment() {
        var marcada = adapter.marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA);

        assertThat(marcada).isTrue();
        var venta = jdbc.statementContaining(MARCAR_VENTA);
        assertThat(venta.sql()).contains("estado = 'ANULADA'").contains("AND estado = 'CONFIRMADA'")
                .contains("AND es_activo = '1'");
        assertThat(venta.params())
                .containsEntry("ventaId", VENTA).containsEntry("tenantId", TENANT)
                .containsEntry("actorId", ACTOR_ID).containsEntry("actor", ACTOR.codigo())
                .containsEntry("motivo", "Error de cobro").containsEntry("fecha", MOMENTO);
        var pago = jdbc.statementContaining(REVERSAR_PAGO);
        assertThat(pago.sql()).contains("estado = 'REVERSADO'")
                .contains("p.tenant_id = v.tenant_id AND p.venta_id = v.id")
                .contains("p.es_activo = '1' AND p.estado = 'CONFIRMADO'");
        assertThat(pago.params()).containsEntry("ventaId", VENTA).containsEntry("tenantId", TENANT);
    }

    @Test
    void whenTheSaleIsNoLongerConfirmedItReportsFalseWithoutTouchingThePayment() {
        jdbc.updates(MARCAR_VENTA, 0);

        assertThat(adapter.marcarAnulada(TENANT, VENTA, ACTOR, "Error de cobro", AHORA)).isFalse();
        assertThat(jdbc.statements()).noneMatch(statement -> statement.sql().contains(REVERSAR_PAGO));
    }
}
