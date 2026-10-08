package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ReferenciasVentasJdbcAdapter implements ReferenciasVentasPort {

    private static final String TERMINAL = """
            SELECT tp.uuid_publico, es.uuid_publico AS establecimiento_uuid,
                   (tp.es_activo = '1' AND tp.estado = 'ACTIVO') AS operable
              FROM sch_organizacion.terminal_pos tp
              JOIN sch_admin.tenant t ON t.id = tp.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = tp.establecimiento_id AND es.tenant_id = tp.tenant_id
             WHERE t.uuid_publico = :tenantId AND tp.uuid_publico = :terminalId
            """;

    private static final String ALMACEN = """
            SELECT a.uuid_publico, s.uuid_publico AS establecimiento_uuid,
                   (a.es_activo = '1' AND a.permite_lotes AND a.permite_venta) AS operable
              FROM sch_organizacion.almacen a
              JOIN sch_admin.tenant t ON t.id = a.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico s
                ON s.id = a.establecimiento_id AND s.tenant_id = a.tenant_id
             WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :almacenId
            """;
    private static final String SKUS = """
            SELECT k.uuid_publico, k.descripcion_comercial, k.unidad_venta_codigo, k.permite_venta_fraccion,
                   (k.es_activo = '1' AND k.estado_comercial = 'ACTIVO') AS operable
              FROM sch_catalogo.sku_comercial k
              JOIN sch_admin.tenant t ON t.id = k.tenant_id
             WHERE t.uuid_publico = :tenantId AND k.uuid_publico IN (:skuIds)
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
                        rs.getBoolean("operable")))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlmacenRef> almacen(UUID tenantId, UUID almacenId) {
        return jdbcClient.sql(ALMACEN)
                .param("tenantId", tenantId)
                .param("almacenId", almacenId)
                .query((rs, rowNumber) -> new AlmacenRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "establecimiento_uuid"),
                        rs.getBoolean("operable")))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, SkuVentaRef> skus(UUID tenantId, Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) return Map.of();
        return jdbcClient.sql(SKUS)
                .param("tenantId", tenantId)
                .param("skuIds", skuIds)
                .query((rs, rowNumber) -> new SkuVentaRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("descripcion_comercial"),
                        rs.getString("unidad_venta_codigo"), rs.getBoolean("permite_venta_fraccion"),
                        rs.getBoolean("operable")))
                .list()
                .stream()
                .collect(Collectors.toMap(SkuVentaRef::id, Function.identity()));
    }
}
