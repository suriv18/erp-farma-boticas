package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort;
import com.softprimesolutions.compras.infrastructure.persistence.JdbcColumns;
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
public class ComprasReferenciasJdbcAdapter implements ReferenciasComprasPort {

    private static final String ESTABLECIMIENTO = """
            SELECT es.uuid_publico, e.uuid_publico AS empresa_uuid,
                   (es.es_activo = '1' AND es.estado_operativo = 'ACTIVO') AS operable
              FROM sch_organizacion.establecimiento_farmaceutico es
              JOIN sch_admin.tenant t ON t.id = es.tenant_id
              JOIN sch_organizacion.empresa_operadora e ON e.id = es.empresa_id AND e.tenant_id = es.tenant_id
             WHERE t.uuid_publico = :tenantId AND es.uuid_publico = :referenciaId
            """;
    private static final String ALMACEN = """
            SELECT a.uuid_publico, s.uuid_publico AS establecimiento_uuid,
                   (a.es_activo = '1' AND a.permite_lotes) AS operable
              FROM sch_organizacion.almacen a
              JOIN sch_admin.tenant t ON t.id = a.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico s
                ON s.id = a.establecimiento_id AND s.tenant_id = a.tenant_id
             WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :referenciaId
            """;
    private static final String SKUS = """
            SELECT k.uuid_publico, k.descripcion_comercial,
                   (k.es_activo = '1' AND k.estado_comercial = 'ACTIVO') AS operable
              FROM sch_catalogo.sku_comercial k
              JOIN sch_admin.tenant t ON t.id = k.tenant_id
             WHERE t.uuid_publico = :tenantId AND k.uuid_publico IN (:skuIds)
            """;
    private static final String UNIDAD_MEDIDA = """
            SELECT EXISTS (SELECT 1 FROM sch_catalogo.unidad_medida WHERE codigo = :codigo)
            """;

    private final JdbcClient jdbcClient;

    public ComprasReferenciasJdbcAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<EstablecimientoRef> establecimiento(UUID tenantId, UUID establecimientoId) {
        return jdbcClient.sql(ESTABLECIMIENTO)
                .param("tenantId", tenantId)
                .param("referenciaId", establecimientoId)
                .query((rs, rowNumber) -> new EstablecimientoRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "empresa_uuid"),
                        rs.getBoolean("operable")))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AlmacenRef> almacen(UUID tenantId, UUID almacenId) {
        return jdbcClient.sql(ALMACEN)
                .param("tenantId", tenantId)
                .param("referenciaId", almacenId)
                .query((rs, rowNumber) -> new AlmacenRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "establecimiento_uuid"),
                        rs.getBoolean("operable")))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Map<UUID, SkuRef> skus(UUID tenantId, Collection<UUID> skuIds) {
        if (skuIds.isEmpty()) return Map.of();
        return jdbcClient.sql(SKUS)
                .param("tenantId", tenantId)
                .param("skuIds", skuIds)
                .query((rs, rowNumber) -> new SkuRef(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("descripcion_comercial"),
                        rs.getBoolean("operable")))
                .list()
                .stream()
                .collect(Collectors.toMap(SkuRef::id, Function.identity()));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existeUnidadMedida(String codigo) {
        return jdbcClient.sql(UNIDAD_MEDIDA).param("codigo", codigo).query(Boolean.class).single();
    }
}
