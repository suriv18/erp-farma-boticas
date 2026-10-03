package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import java.util.Map;
import java.util.Collection;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ReferenciasVentasJdbcAdapter implements ReferenciasVentasPort {

    private static final String TERMINAL = """
            SELECT tp.uuid_publico, es.uuid_publico AS establecimiento_uuid, tp.codigo,
                   (tp.es_activo = '1' AND tp.estado = 'ACTIVO') AS operable
              FROM sch_organizacion.terminal_pos tp
              JOIN sch_admin.tenant t ON t.id = tp.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = tp.establecimiento_id AND es.tenant_id = tp.tenant_id
             WHERE t.uuid_publico = :tenantId AND tp.uuid_publico = :terminalId
            """;

    private final JdbcClient jdbcClient;

    public ReferenciasVentasJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TerminalRef> terminal(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(TERMINAL)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> new TerminalRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "establecimiento_uuid"),
                        rs.getString("codigo"), rs.getBoolean("operable")))
                .optional();
    }

    @Override
    public Optional<AlmacenRef> almacen(UUID tenantId, UUID almacenId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Map<UUID, SkuVentaRef> skus(UUID tenantId, Collection<UUID> skuIds) {
        throw new UnsupportedOperationException();
    }
}
