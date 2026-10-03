package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository("ventasNumeracionAdapter")
public class NumeracionJdbcAdapter implements NumeracionPort {

    private static final String SIGUIENTE = """
            INSERT INTO sch_venta.secuencia_operacion (tenant_id, terminal_id, ultimo_numero)
            SELECT tp.tenant_id, tp.id, 1
              FROM sch_organizacion.terminal_pos tp
              JOIN sch_admin.tenant t ON t.id = tp.tenant_id
             WHERE t.uuid_publico = :tenantId AND tp.uuid_publico = :terminalId
            ON CONFLICT (tenant_id, terminal_id)
            DO UPDATE SET ultimo_numero = sch_venta.secuencia_operacion.ultimo_numero + 1
            RETURNING ultimo_numero
            """;

    private final JdbcClient jdbcClient;

    public NumeracionJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public String siguienteNumeroOperacion(UUID tenantId, UUID terminalId, String codigoTerminal) {
        var numero = jdbcClient.sql(SIGUIENTE)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query(Long.class)
                .single();
        return String.format("%s-%06d", codigoTerminal, numero);
    }
}
