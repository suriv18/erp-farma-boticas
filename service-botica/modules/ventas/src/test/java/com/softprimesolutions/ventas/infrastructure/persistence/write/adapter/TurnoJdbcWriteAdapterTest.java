package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.VentasFixtures.ACTOR;
import static com.softprimesolutions.ventas.VentasFixtures.ACTOR_ID;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.TURNO;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.turno;
import static com.softprimesolutions.ventas.infrastructure.persistence.Rows.MOMENTO;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcClientStub;
import com.softprimesolutions.ventas.infrastructure.persistence.Rows;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.JdbcUpdateAffectedIncorrectNumberOfRowsException;
import org.springframework.transaction.support.TransactionOperations;

class TurnoJdbcWriteAdapterTest {

    private static final String INSERT = "INSERT INTO sch_venta.turno_caja";
    private static final String UPDATE = "UPDATE sch_venta.turno_caja";

    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final TurnoJdbcWriteAdapter adapter =
            new TurnoJdbcWriteAdapter(jdbc.client(), TransactionOperations.withoutTransaction());

    @Test
    void insertsTheTurnoResolvingTheTerminalAndTheCashierFromTheirPublicIds() {
        var outcome = adapter.insertar(turno(EstadoTurno.ABIERTO, "50.00"));

        assertThat(outcome).isEqualTo(GuardadoOutcome.GUARDADO);
        assertThat(jdbc.statementContaining(INSERT).params())
                .containsEntry("turnoId", TURNO).containsEntry("tenantId", TENANT)
                .containsEntry("terminalId", TERMINAL).containsEntry("cajeroId", ACTOR_ID)
                .containsEntry("aperturaAt", MOMENTO).containsEntry("fondoInicial", dec("50.00"))
                .containsEntry("actor", ACTOR.codigo());
    }

    @Test
    void onlyTheOpenTurnoPerTerminalUniqueViolationIsReportedAsDuplicate() {
        jdbc.failsWith(INSERT, JdbcClientStub.unicidadViolada("uk_turno_terminal_abierto"));

        assertThat(adapter.insertar(turno(EstadoTurno.ABIERTO, "50.00"))).isEqualTo(GuardadoOutcome.DUPLICADO);
    }

    @Test
    void anyOtherIntegrityViolationPropagates() {
        var otraUnicidad = JdbcClientStub.unicidadViolada("uk_turno_caja_uuid");
        jdbc.failsWith(INSERT, otraUnicidad);
        assertThatThrownBy(() -> adapter.insertar(turno(EstadoTurno.ABIERTO, "50.00"))).isSameAs(otraUnicidad);

        var sinCajero = new DataIntegrityViolationException("null value in column \"cajero_usuario_id\"");
        var conNulo = new JdbcClientStub().failsWith(INSERT, sinCajero);
        var otro = new TurnoJdbcWriteAdapter(conNulo.client(), TransactionOperations.withoutTransaction());
        assertThatThrownBy(() -> otro.insertar(turno(EstadoTurno.ABIERTO, "50.00"))).isSameAs(sinCajero);
    }

    @Test
    void anInsertThatTouchesNoRowPropagatesInsteadOfLookingLikeADuplicate() {
        var sinFilas = new JdbcClientStub().updates(INSERT, 0);
        var otro = new TurnoJdbcWriteAdapter(sinFilas.client(), TransactionOperations.withoutTransaction());

        assertThatThrownBy(() -> otro.insertar(turno(EstadoTurno.ABIERTO, "50.00")))
                .isInstanceOf(JdbcUpdateAffectedIncorrectNumberOfRowsException.class);
    }

    @Test
    void locksTheTurnoRowWhenLoadingItForUpdate() {
        jdbc.rows("FOR UPDATE OF tc", Rows.turno());

        var encontrado = adapter.findPorIdParaActualizar(TENANT, TURNO).orElseThrow();

        assertThat(encontrado.id()).isEqualTo(TURNO);
        assertThat(encontrado.tenantId()).isEqualTo(TENANT);
        assertThat(encontrado.estado()).isEqualTo(EstadoTurno.ABIERTO);
        assertThat(encontrado.fondoInicial()).isEqualTo(dec("50.00"));
        assertThat(jdbc.statementContaining("FOR UPDATE OF tc").params())
                .containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO);
    }

    @Test
    void aMissingTurnoIsEmpty() {
        assertThat(adapter.findPorIdParaActualizar(TENANT, TURNO)).isEmpty();
    }

    @Test
    void sumsTheConfirmedCashPaymentsOfTheTurno() {
        jdbc.scalar("COALESCE(SUM(p.monto), 0)", dec("40.00"));

        assertThat(adapter.totalVentasEfectivo(TENANT, TURNO)).isEqualTo(dec("40.00"));
        assertThat(jdbc.statementContaining("COALESCE(SUM(p.monto), 0)").params())
                .containsEntry("tenantId", TENANT).containsEntry("turnoId", TURNO);
    }

    @Test
    void updatesTheCloseOnlyWhileTheTurnoIsOpen() {
        var cerrado = turno(EstadoTurno.ABIERTO, "100.00")
                .cerrar(dec("40"), dec("135"), "Cierre", MOMENTO.toInstant())
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(adapter.actualizarCierre(cerrado, ACTOR)).isTrue();

        var statement = jdbc.statementContaining(UPDATE);
        assertThat(statement.sql()).contains("estado = 'ABIERTO'");
        assertThat(statement.params())
                .containsEntry("turnoId", TURNO).containsEntry("tenantId", TENANT)
                .containsEntry("cierreAt", MOMENTO).containsEntry("totalVentas", dec("40.00"))
                .containsEntry("totalSistema", dec("140.00")).containsEntry("totalDeclarado", dec("135.00"))
                .containsEntry("diferencia", dec("-5.00")).containsEntry("observacion", "Cierre")
                .containsEntry("actor", ACTOR.codigo());
    }

    @Test
    void reportsALostUpdateWhenNoRowIsTouched() {
        jdbc.updates(UPDATE, 0);
        var cerrado = turno(EstadoTurno.ABIERTO, "100.00")
                .cerrar(dec("0"), dec("100"), null, MOMENTO.toInstant())
                .fold(value -> value, error -> { throw new AssertionError(error); });

        assertThat(adapter.actualizarCierre(cerrado, ACTOR)).isFalse();
    }

    @Test
    void locksTheOpenTurnoOfATerminalForShareSoACloseWaitsForInFlightSales() {
        jdbc.rows("FOR SHARE OF tc", Rows.turno());

        var turno = adapter.bloquearTurnoAbierto(TENANT, TERMINAL).orElseThrow();

        assertThat(turno.id()).isEqualTo(TURNO);
        var statement = jdbc.statementContaining("FOR SHARE OF tc");
        assertThat(statement.sql()).contains("tc.estado = 'ABIERTO'");
        assertThat(statement.params()).containsEntry("tenantId", TENANT).containsEntry("terminalId", TERMINAL);
        assertThat(new TurnoJdbcWriteAdapter(new JdbcClientStub().client(), TransactionOperations.withoutTransaction())
                .bloquearTurnoAbierto(TENANT, TERMINAL)).isEmpty();
    }
}
