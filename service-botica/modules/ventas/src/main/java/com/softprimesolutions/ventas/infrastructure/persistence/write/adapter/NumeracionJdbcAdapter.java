package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import java.util.Locale;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository("ventasNumeracionAdapter")
public class NumeracionJdbcAdapter implements NumeracionPort {

    private static final String SIGUIENTE = """
            WITH siguiente AS (
                INSERT INTO sch_venta.secuencia_operacion (tenant_id, terminal_id, ultimo_numero)
                SELECT tp.tenant_id, tp.id, 1
                  FROM sch_organizacion.terminal_pos tp
                  JOIN sch_admin.tenant t ON t.id = tp.tenant_id
                 WHERE t.uuid_publico = :tenantId AND tp.uuid_publico = :terminalId
                ON CONFLICT (tenant_id, terminal_id)
                DO UPDATE SET ultimo_numero = sch_venta.secuencia_operacion.ultimo_numero + 1
                RETURNING tenant_id, terminal_id, ultimo_numero
            )
            SELECT es.codigo AS establecimiento_codigo, tp.codigo AS terminal_codigo, s.ultimo_numero
              FROM siguiente s
              JOIN sch_organizacion.terminal_pos tp ON tp.tenant_id = s.tenant_id AND tp.id = s.terminal_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.tenant_id = tp.tenant_id AND es.id = tp.establecimiento_id
            """;

    private final JdbcClient jdbcClient;

    public NumeracionJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public String siguienteNumeroOperacion(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(SIGUIENTE)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> String.format(Locale.ROOT, "%s-%s-%06d",
                        rs.getString("establecimiento_codigo"), rs.getString("terminal_codigo"),
                        rs.getLong("ultimo_numero")))
                .single();
    }
}
