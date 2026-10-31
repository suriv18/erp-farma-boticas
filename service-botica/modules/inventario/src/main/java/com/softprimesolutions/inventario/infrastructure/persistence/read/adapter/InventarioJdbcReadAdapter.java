package com.softprimesolutions.inventario.infrastructure.persistence.read.adapter;

import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.dto.result.PaginaResult;
import com.softprimesolutions.inventario.application.dto.result.PosicionResult;
import com.softprimesolutions.inventario.application.port.out.InventarioReadPort;
import com.softprimesolutions.inventario.infrastructure.persistence.JdbcColumns;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class InventarioJdbcReadAdapter implements InventarioReadPort {

    private static final String POSICION_SELECT = """
            SELECT p.uuid_publico, s.uuid_publico AS establecimiento_uuid, a.uuid_publico AS almacen_uuid,
                   k.uuid_publico AS sku_uuid, l.uuid_publico AS lote_uuid, l.numero_lote,
                   l.fecha_vencimiento, l.estado_lote, p.estado_inventario, p.cantidad_fisica,
                   p.cantidad_reservada, p.cantidad_disponible,
                   (l.estado_lote = 'HABILITADO' AND p.estado_inventario = 'DISPONIBLE'
                    AND l.fecha_vencimiento >= CURRENT_DATE) AS vendible,
                   p.version_lock, p.ultimo_movimiento_at
            """;
    private static final String POSICION_FROM = """
            FROM sch_inventario.posicion_inventario p
            JOIN sch_admin.tenant t ON t.id = p.tenant_id
            JOIN sch_organizacion.establecimiento_farmaceutico s
              ON s.id = p.establecimiento_id AND s.tenant_id = p.tenant_id
            JOIN sch_organizacion.almacen a ON a.id = p.almacen_id AND a.tenant_id = p.tenant_id
            JOIN sch_catalogo.sku_comercial k ON k.id = p.sku_id AND k.tenant_id = p.tenant_id
            JOIN sch_inventario.lote l ON l.id = p.lote_id AND l.tenant_id = p.tenant_id
            WHERE t.uuid_publico = :tenantId AND p.es_activo = '1'
              AND (CAST(:establecimientoId AS uuid) IS NULL OR s.uuid_publico = CAST(:establecimientoId AS uuid))
              AND (CAST(:almacenId AS uuid) IS NULL OR a.uuid_publico = CAST(:almacenId AS uuid))
              AND (CAST(:skuId AS uuid) IS NULL OR k.uuid_publico = CAST(:skuId AS uuid))
            """;
    private static final String POSICION_ORDER = """
            ORDER BY l.fecha_vencimiento, l.numero_lote, p.id
            LIMIT :limit OFFSET :offset
            """;
    private static final String LOTE_SELECT = """
            SELECT l.uuid_publico, k.uuid_publico AS sku_uuid, l.numero_lote, l.fecha_vencimiento,
                   l.estado_lote, l.motivo_estado, l.bloqueado_at,
                   (l.estado_lote = 'HABILITADO' AND l.fecha_vencimiento >= CURRENT_DATE) AS vendible
              FROM sch_inventario.lote l
              JOIN sch_admin.tenant t ON t.id = l.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.id = l.sku_id AND k.tenant_id = l.tenant_id
             WHERE t.uuid_publico = :tenantId AND l.uuid_publico = :loteId AND l.es_activo = '1'
            """;

    private final JdbcClient jdbcClient;

    public InventarioJdbcReadAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<PosicionResult> findPosiciones(
            UUID tenantId, UUID establecimientoId, UUID almacenId, UUID skuId, int page, int size) {
        var items = filtrada(POSICION_SELECT + POSICION_FROM + POSICION_ORDER,
                        tenantId, establecimientoId, almacenId, skuId)
                .param("limit", size)
                .param("offset", page * size)
                .query(InventarioJdbcReadAdapter::mapPosicion)
                .list();
        var total = filtrada("SELECT COUNT(*) " + POSICION_FROM, tenantId, establecimientoId, almacenId, skuId)
                .query(Long.class)
                .single();
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LoteResult> findLoteById(UUID tenantId, UUID loteId) {
        return jdbcClient.sql(LOTE_SELECT)
                .param("tenantId", tenantId)
                .param("loteId", loteId)
                .query(InventarioJdbcReadAdapter::mapLote)
                .optional();
    }

    private JdbcClient.StatementSpec filtrada(
            String sql, UUID tenantId, UUID establecimientoId, UUID almacenId, UUID skuId) {
        return jdbcClient.sql(sql)
                .param("tenantId", tenantId)
                .param("establecimientoId", establecimientoId)
                .param("almacenId", almacenId)
                .param("skuId", skuId);
    }

    private static PosicionResult mapPosicion(ResultSet rs, int rowNumber) throws SQLException {
        return new PosicionResult(
                JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "establecimiento_uuid"),
                JdbcColumns.uuid(rs, "almacen_uuid"), JdbcColumns.uuid(rs, "sku_uuid"),
                JdbcColumns.uuid(rs, "lote_uuid"), rs.getString("numero_lote"),
                JdbcColumns.date(rs, "fecha_vencimiento"), rs.getString("estado_lote"),
                rs.getString("estado_inventario"), rs.getBigDecimal("cantidad_fisica"),
                rs.getBigDecimal("cantidad_reservada"), rs.getBigDecimal("cantidad_disponible"),
                rs.getBoolean("vendible"), rs.getLong("version_lock"),
                JdbcColumns.instant(rs, "ultimo_movimiento_at"));
    }

    private static LoteResult mapLote(ResultSet rs, int rowNumber) throws SQLException {
        return new LoteResult(
                JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "sku_uuid"),
                rs.getString("numero_lote"), JdbcColumns.date(rs, "fecha_vencimiento"),
                rs.getString("estado_lote"), rs.getString("motivo_estado"),
                JdbcColumns.instant(rs, "bloqueado_at"), rs.getBoolean("vendible"));
    }
}
