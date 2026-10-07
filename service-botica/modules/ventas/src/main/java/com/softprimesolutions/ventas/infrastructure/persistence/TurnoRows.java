package com.softprimesolutions.ventas.infrastructure.persistence;

import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.TurnoCaja;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class TurnoRows {

    private static final String VENTAS_EN_EFECTIVO = """
            (SELECT COALESCE(SUM(p.monto), 0)
               FROM sch_venta.pago_venta p
               JOIN sch_venta.venta v ON v.tenant_id = p.tenant_id AND v.id = p.venta_id
              WHERE v.tenant_id = tc.tenant_id AND v.turno_caja_id = tc.id
                AND v.estado = 'CONFIRMADA' AND v.es_activo = '1'
                AND p.estado = 'CONFIRMADO' AND p.es_activo = '1')""";
    public static final String SELECT = """
            SELECT tc.uuid_publico, tp.uuid_publico AS terminal_uuid, es.uuid_publico AS establecimiento_uuid,
                   m.uuid_publico AS cajero_uuid, tc.apertura_at, tc.fondo_inicial, tc.estado, tc.cierre_at,
                   CASE WHEN tc.estado = 'CERRADO' THEN tc.total_ventas_sistema ELSE
            """ + VENTAS_EN_EFECTIVO + """
                   END AS total_ventas_sistema,
                   CASE WHEN tc.estado = 'CERRADO' THEN tc.total_sistema ELSE tc.fondo_inicial + 
            """ + VENTAS_EN_EFECTIVO + """
                   END AS total_sistema,
                   tc.total_declarado, tc.diferencia, tc.observacion_cierre
              FROM sch_venta.turno_caja tc
              JOIN sch_admin.tenant t ON t.id = tc.tenant_id
              JOIN sch_organizacion.terminal_pos tp ON tp.id = tc.terminal_id AND tp.tenant_id = tc.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = tc.establecimiento_id AND es.tenant_id = tc.tenant_id
              JOIN sch_seguridad.membership m ON m.id = tc.cajero_usuario_id
             WHERE t.uuid_publico = :tenantId AND tc.es_activo = '1'
            """;
    public static final String POR_ID = SELECT + " AND tc.uuid_publico = :turnoId";
    public static final String ABIERTO_POR_TERMINAL = SELECT
            + " AND tp.uuid_publico = :terminalId AND tc.estado IN ('ABIERTO', 'EN_ARQUEO')";

    private TurnoRows() {
    }

    public static TurnoCaja map(ResultSet rs, UUID tenantId) throws SQLException {
        return TurnoCaja.restore(
                JdbcColumns.uuid(rs, "uuid_publico"), tenantId, JdbcColumns.uuid(rs, "terminal_uuid"),
                JdbcColumns.uuid(rs, "establecimiento_uuid"), new Actor(JdbcColumns.uuid(rs, "cajero_uuid")),
                JdbcColumns.instant(rs, "apertura_at"), rs.getBigDecimal("fondo_inicial"),
                EstadoTurno.valueOf(rs.getString("estado")), JdbcColumns.instant(rs, "cierre_at"),
                rs.getBigDecimal("total_ventas_sistema"), rs.getBigDecimal("total_sistema"),
                rs.getBigDecimal("total_declarado"), rs.getBigDecimal("diferencia"),
                rs.getString("observacion_cierre"));
    }
}
