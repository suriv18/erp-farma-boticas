package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import com.softprimesolutions.compras.application.port.out.NumeracionPort;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class NumeracionJdbcAdapter implements NumeracionPort {

    private static final String ORDEN = """
            SELECT 'OC-' || to_char(CURRENT_DATE, 'YYYY') || '-'
                   || lpad(nextval('sch_abastecimiento.seq_orden_compra_numero')::text, 6, '0')
            """;
    private static final String RECEPCION = """
            SELECT 'REC-' || to_char(CURRENT_DATE, 'YYYY') || '-'
                   || lpad(nextval('sch_abastecimiento.seq_recepcion_compra_numero')::text, 6, '0')
            """;

    private final JdbcClient jdbcClient;

    public NumeracionJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public String siguienteNumeroOrden() {
        return jdbcClient.sql(ORDEN).query(String.class).single();
    }

    @Override
    public String siguienteNumeroRecepcion() {
        return jdbcClient.sql(RECEPCION).query(String.class).single();
    }
}
