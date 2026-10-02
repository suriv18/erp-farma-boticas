package com.softprimesolutions.inventario.infrastructure.persistence.write.adapter;

import com.softprimesolutions.inventario.application.port.out.ReferenciasInventarioPort;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class InventarioReferenciasJdbcAdapter implements ReferenciasInventarioPort {

    private static final String ALMACEN = """
            SELECT CASE WHEN a.es_activo = '1' AND a.permite_lotes THEN 'OPERABLE' ELSE 'NO_OPERABLE' END
              FROM sch_organizacion.almacen a
              JOIN sch_admin.tenant t ON t.id = a.tenant_id
             WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :referenciaId
            """;
    private static final String SKU = """
            SELECT CASE WHEN k.es_activo = '1' AND k.estado_comercial = 'ACTIVO'
                        THEN 'OPERABLE' ELSE 'NO_OPERABLE' END
              FROM sch_catalogo.sku_comercial k
              JOIN sch_admin.tenant t ON t.id = k.tenant_id
             WHERE t.uuid_publico = :tenantId AND k.uuid_publico = :referenciaId
            """;

    private final JdbcClient jdbcClient;

    public InventarioReferenciasJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoReferencia estadoAlmacen(UUID tenantId, UUID almacenId) {
        return estado(ALMACEN, tenantId, almacenId);
    }

    @Override
    @Transactional(readOnly = true)
    public EstadoReferencia estadoSku(UUID tenantId, UUID skuId) {
        return estado(SKU, tenantId, skuId);
    }

    private EstadoReferencia estado(String sql, UUID tenantId, UUID referenciaId) {
        return jdbcClient.sql(sql)
                .param("tenantId", tenantId)
                .param("referenciaId", referenciaId)
                .query(String.class)
                .optional()
                .map(EstadoReferencia::valueOf)
                .orElse(EstadoReferencia.INEXISTENTE);
    }
}
