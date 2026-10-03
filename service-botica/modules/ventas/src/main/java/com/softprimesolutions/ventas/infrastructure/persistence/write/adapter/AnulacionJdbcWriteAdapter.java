package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.domain.model.EstadoTurno;
import com.softprimesolutions.ventas.domain.model.EstadoVenta;
import com.softprimesolutions.ventas.domain.valueobject.Actor;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class AnulacionJdbcWriteAdapter implements AnulacionWritePort {

    private static final String BLOQUEAR = """
            SELECT v.uuid_publico, tc.uuid_publico AS turno_uuid, v.estado AS venta_estado,
                   tc.estado AS turno_estado
              FROM sch_venta.venta v
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_venta.turno_caja tc ON tc.id = v.turno_caja_id AND tc.tenant_id = v.tenant_id
             WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId AND v.es_activo = '1'
               FOR UPDATE OF v FOR SHARE OF tc
            """;
    private static final String MARCAR_VENTA = """
            UPDATE sch_venta.venta
               SET estado = 'ANULADA', anulada_at = :fecha,
                   anulada_por_usuario_id = (SELECT m.id FROM sch_seguridad.membership m
                                              WHERE m.uuid_publico = :actorId AND m.tenant_id = venta.tenant_id),
                   motivo_anulacion = :motivo, version_lock = version_lock + 1, updated_at = :fecha,
                   updated_by = :actor
             WHERE uuid_publico = :ventaId AND estado = 'CONFIRMADA'
               AND tenant_id = (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)
            """;
    private static final String REVERSAR_PAGO = """
            UPDATE sch_venta.pago_venta
               SET estado = 'REVERSADO'
             WHERE venta_id = (SELECT v.id FROM sch_venta.venta v
                                 JOIN sch_admin.tenant t ON t.id = v.tenant_id
                                WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId)
               AND estado = 'CONFIRMADO'
            """;

    private final JdbcClient jdbcClient;

    public AnulacionJdbcWriteAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<VentaParaAnular> bloquearVenta(UUID tenantId, UUID ventaId) {
        return jdbcClient.sql(BLOQUEAR)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> new VentaParaAnular(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "turno_uuid"),
                        EstadoVenta.valueOf(rs.getString("venta_estado")),
                        EstadoTurno.valueOf(rs.getString("turno_estado"))))
                .optional();
    }

    @Override
    public boolean marcarAnulada(UUID tenantId, UUID ventaId, Actor actor, String motivo, Instant ahora) {
        var actualizadas = jdbcClient.sql(MARCAR_VENTA)
                .param("ventaId", ventaId)
                .param("tenantId", tenantId)
                .param("actorId", actor.id())
                .param("actor", actor.codigo())
                .param("motivo", motivo)
                .param("fecha", JdbcColumns.offset(ahora))
                .update();
        if (actualizadas != 1) return false;
        jdbcClient.sql(REVERSAR_PAGO)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .update();
        return true;
    }
}
