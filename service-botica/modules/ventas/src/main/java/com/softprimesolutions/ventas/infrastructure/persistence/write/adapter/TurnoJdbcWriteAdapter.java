package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import com.softprimesolutions.ventas.infrastructure.persistence.TurnoRows;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

@Repository
public class TurnoJdbcWriteAdapter implements TurnoWritePort {

    private static final String POR_ID_PARA_ACTUALIZAR = TurnoRows.POR_ID + " FOR UPDATE OF tc";
    private static final String ABIERTO_PARA_COMPARTIR = TurnoRows.SELECT
            + " AND tp.uuid_publico = :terminalId AND tc.estado = 'ABIERTO' FOR SHARE OF tc";
    private static final String INSERTAR = """
            INSERT INTO sch_venta.turno_caja
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, terminal_id, cajero_usuario_id,
                 apertura_at, fondo_inicial, estado, total_ventas_sistema, total_ingresos_sistema,
                 total_retiros_sistema, total_sistema, created_at, created_by)
            SELECT :turnoId, tp.tenant_id, tp.empresa_id, tp.establecimiento_id, tp.id,
                   (SELECT m.id FROM sch_seguridad.membership m
                     WHERE m.uuid_publico = :cajeroId AND m.tenant_id = tp.tenant_id),
                   :aperturaAt, :fondoInicial, 'ABIERTO', 0, 0, 0, :fondoInicial, :aperturaAt, :actor
              FROM sch_organizacion.terminal_pos tp
              JOIN sch_admin.tenant t ON t.id = tp.tenant_id
             WHERE t.uuid_publico = :tenantId AND tp.uuid_publico = :terminalId
            """;
    private static final String TOTAL_VENTAS = """
            SELECT COALESCE(SUM(p.monto), 0)
              FROM sch_venta.pago_venta p
              JOIN sch_venta.venta v ON v.tenant_id = p.tenant_id AND v.id = p.venta_id
              JOIN sch_venta.turno_caja tc ON tc.tenant_id = v.tenant_id AND tc.id = v.turno_caja_id
              JOIN sch_admin.tenant t ON t.id = tc.tenant_id
             WHERE t.uuid_publico = :tenantId AND tc.uuid_publico = :turnoId
               AND v.estado = 'CONFIRMADA' AND v.es_activo = '1'
               AND p.estado = 'CONFIRMADO' AND p.es_activo = '1'
            """;
    private static final String ACTUALIZAR_CIERRE = """
            UPDATE sch_venta.turno_caja
               SET estado = 'CERRADO', cierre_at = :cierreAt, total_ventas_sistema = :totalVentas,
                   total_sistema = :totalSistema, total_declarado = :totalDeclarado, diferencia = :diferencia,
                   observacion_cierre = :observacion, updated_at = :cierreAt, updated_by = :actor
             WHERE uuid_publico = :turnoId AND estado = 'ABIERTO'
               AND tenant_id = (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transaction;

    public TurnoJdbcWriteAdapter(JdbcClient jdbcClient, TransactionOperations transaction) {
        this.jdbcClient = jdbcClient;
        this.transaction = transaction;
    }

    @Override
    public GuardadoOutcome insertar(TurnoCaja turno) {
        try {
            transaction.executeWithoutResult(status -> exigirUnaFila(insertarFila(turno)));
            return GuardadoOutcome.GUARDADO;
        } catch (DataIntegrityViolationException | FilaNoInsertada exception) {
            return GuardadoOutcome.DUPLICADO;
        }
    }

    @Override
    public Optional<TurnoCaja> findPorIdParaActualizar(UUID tenantId, UUID turnoId) {
        return jdbcClient.sql(POR_ID_PARA_ACTUALIZAR)
                .param("tenantId", tenantId)
                .param("turnoId", turnoId)
                .query((rs, rowNumber) -> TurnoRows.map(rs, tenantId))
                .optional();
    }

    @Override
    public Optional<TurnoCaja> bloquearTurnoAbierto(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(ABIERTO_PARA_COMPARTIR)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> TurnoRows.map(rs, tenantId))
                .optional();
    }

    @Override
    public BigDecimal totalVentasEfectivo(UUID tenantId, UUID turnoId) {
        return jdbcClient.sql(TOTAL_VENTAS)
                .param("tenantId", tenantId)
                .param("turnoId", turnoId)
                .query(BigDecimal.class)
                .single();
    }

    @Override
    public boolean actualizarCierre(TurnoCaja turno, Actor actor) {
        return jdbcClient.sql(ACTUALIZAR_CIERRE)
                .param("turnoId", turno.id())
                .param("tenantId", turno.tenantId())
                .param("cierreAt", JdbcColumns.offset(turno.cierreAt()))
                .param("totalVentas", turno.totalVentasSistema())
                .param("totalSistema", turno.totalSistema())
                .param("totalDeclarado", turno.totalDeclarado())
                .param("diferencia", turno.diferencia())
                .param("observacion", turno.observacionCierre())
                .param("actor", actor.codigo())
                .update() == 1;
    }

    private int insertarFila(TurnoCaja turno) {
        return jdbcClient.sql(INSERTAR)
                .param("turnoId", turno.id())
                .param("tenantId", turno.tenantId())
                .param("terminalId", turno.terminalId())
                .param("cajeroId", turno.cajero().id())
                .param("aperturaAt", JdbcColumns.offset(turno.aperturaAt()))
                .param("fondoInicial", turno.fondoInicial())
                .param("actor", turno.cajero().codigo())
                .update();
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) throw new FilaNoInsertada();
    }

    private static final class FilaNoInsertada extends RuntimeException {

        private static final long serialVersionUID = 1L;

        FilaNoInsertada() {
            super(null, null, false, false);
        }
    }
}
