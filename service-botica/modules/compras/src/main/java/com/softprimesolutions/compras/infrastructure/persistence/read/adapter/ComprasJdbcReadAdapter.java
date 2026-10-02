package com.softprimesolutions.compras.infrastructure.persistence.read.adapter;

import com.softprimesolutions.compras.application.dto.result.LineaRecepcionResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResumenResult;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.compras.infrastructure.persistence.JdbcColumns;
import com.softprimesolutions.compras.infrastructure.persistence.OrdenCompraRows;
import com.softprimesolutions.compras.infrastructure.persistence.ProveedorRows;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ComprasJdbcReadAdapter implements ComprasReadPort {

    private static final String PROVEEDOR_POR_ID = ProveedorRows.SELECT + " AND p.uuid_publico = :proveedorId";
    private static final String PROVEEDORES_FILTRO = """
             AND (CAST(:estado AS text) IS NULL OR p.estado = CAST(:estado AS text))
             AND (CAST(:texto AS text) IS NULL
                  OR p.razon_social ILIKE '%' || CAST(:texto AS text) || '%'
                  OR p.nombre_comercial ILIKE '%' || CAST(:texto AS text) || '%'
                  OR p.numero_documento LIKE CAST(:texto AS text) || '%')
            """;
    private static final String PROVEEDORES_ORDEN = " ORDER BY p.razon_social, p.id LIMIT :limit OFFSET :offset";
    private static final String ORDENES_SELECT = """
            SELECT o.uuid_publico, o.numero, pr.uuid_publico AS proveedor_uuid, pr.razon_social,
                   es.uuid_publico AS establecimiento_uuid, o.fecha_emision, o.fecha_entrega_estimada,
                   o.moneda, o.total, o.estado
            """;
    private static final String ORDENES_FROM = """
              FROM sch_abastecimiento.orden_compra o
              JOIN sch_admin.tenant t ON t.id = o.tenant_id
              JOIN sch_abastecimiento.proveedor pr ON pr.id = o.proveedor_id AND pr.tenant_id = o.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = o.establecimiento_destino_id AND es.tenant_id = o.tenant_id
             WHERE t.uuid_publico = :tenantId AND o.es_activo = '1'
               AND (CAST(:proveedorId AS uuid) IS NULL OR pr.uuid_publico = CAST(:proveedorId AS uuid))
               AND (CAST(:estado AS text) IS NULL OR o.estado = CAST(:estado AS text))
            """;
    private static final String ORDENES_ORDEN = " ORDER BY o.fecha_emision DESC, o.id DESC LIMIT :limit OFFSET :offset";
    private static final String RECEPCION = """
            SELECT r.uuid_publico, r.numero, o.uuid_publico AS orden_uuid, pr.uuid_publico AS proveedor_uuid,
                   es.uuid_publico AS establecimiento_uuid, a.uuid_publico AS almacen_uuid,
                   r.documento_proveedor_tipo, r.documento_proveedor_serie, r.documento_proveedor_numero,
                   r.guia_remision_remitente, r.guia_remision_transportista, r.fecha_recepcion,
                   r.temperatura_recepcion_c, r.humedad_relativa_pct, r.estado, r.observacion
              FROM sch_abastecimiento.recepcion_compra r
              JOIN sch_admin.tenant t ON t.id = r.tenant_id
              LEFT JOIN sch_abastecimiento.orden_compra o ON o.id = r.orden_compra_id AND o.tenant_id = r.tenant_id
              JOIN sch_abastecimiento.proveedor pr ON pr.id = r.proveedor_id AND pr.tenant_id = r.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = r.establecimiento_id AND es.tenant_id = r.tenant_id
              JOIN sch_organizacion.almacen a ON a.id = r.almacen_id AND a.tenant_id = r.tenant_id
             WHERE t.uuid_publico = :tenantId AND r.uuid_publico = :recepcionId AND r.es_activo = '1'
            """;
    private static final String RECEPCION_LINEAS = """
            SELECT rl.uuid_publico, rl.numero_linea, ol.numero_linea AS numero_linea_orden,
                   k.uuid_publico AS sku_uuid, rl.numero_lote, rl.fecha_fabricacion, rl.fecha_vencimiento,
                   rl.cantidad_recibida, rl.cantidad_aceptada, rl.cantidad_rechazada, rl.costo_unitario,
                   rl.decision_calidad, rl.motivo_decision, rl.observacion, l.uuid_publico AS lote_uuid
              FROM sch_abastecimiento.recepcion_compra_linea rl
              JOIN sch_abastecimiento.recepcion_compra r ON r.id = rl.recepcion_id AND r.tenant_id = rl.tenant_id
              JOIN sch_admin.tenant t ON t.id = r.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.id = rl.sku_id AND k.tenant_id = rl.tenant_id
              LEFT JOIN sch_abastecimiento.orden_compra_linea ol ON ol.id = rl.orden_compra_linea_id
              LEFT JOIN sch_inventario.lote l
                ON l.tenant_id = rl.tenant_id AND l.sku_id = rl.sku_id AND l.numero_lote = rl.numero_lote
               AND l.fecha_vencimiento = rl.fecha_vencimiento AND l.es_activo = '1'
             WHERE t.uuid_publico = :tenantId AND r.uuid_publico = :recepcionId AND rl.es_activo = '1'
             ORDER BY rl.numero_linea
            """;

    private final JdbcClient jdbcClient;

    public ComprasJdbcReadAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProveedorResult> findProveedor(UUID tenantId, UUID proveedorId) {
        return jdbcClient.sql(PROVEEDOR_POR_ID)
                .param("tenantId", tenantId)
                .param("proveedorId", proveedorId)
                .query((rs, rowNumber) -> ComprasApplicationMapper.toResult(ProveedorRows.map(rs, tenantId)))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<ProveedorResult> findProveedores(
            UUID tenantId, String estado, String texto, int page, int size) {
        var filtrado = ProveedorRows.SELECT + PROVEEDORES_FILTRO;
        var items = jdbcClient.sql(filtrado + PROVEEDORES_ORDEN)
                .param("tenantId", tenantId)
                .param("estado", estado)
                .param("texto", texto)
                .param("limit", size)
                .param("offset", page * size)
                .query((rs, rowNumber) -> ComprasApplicationMapper.toResult(ProveedorRows.map(rs, tenantId)))
                .list();
        var total = jdbcClient.sql("SELECT COUNT(*) FROM (" + filtrado + ") filtrados")
                .param("tenantId", tenantId)
                .param("estado", estado)
                .param("texto", texto)
                .query(Long.class)
                .single();
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<OrdenCompraResult> findOrden(UUID tenantId, UUID ordenId) {
        return OrdenCompraRows.buscar(jdbcClient, tenantId, ordenId, false).map(ComprasApplicationMapper::toResult);
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<OrdenCompraResumenResult> findOrdenes(
            UUID tenantId, UUID proveedorId, String estado, int page, int size) {
        var items = jdbcClient.sql(ORDENES_SELECT + ORDENES_FROM + ORDENES_ORDEN)
                .param("tenantId", tenantId)
                .param("proveedorId", proveedorId)
                .param("estado", estado)
                .param("limit", size)
                .param("offset", page * size)
                .query((rs, rowNumber) -> mapResumen(rs))
                .list();
        var total = jdbcClient.sql("SELECT COUNT(*) " + ORDENES_FROM)
                .param("tenantId", tenantId)
                .param("proveedorId", proveedorId)
                .param("estado", estado)
                .query(Long.class)
                .single();
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<RecepcionResult> findRecepcion(UUID tenantId, UUID recepcionId) {
        return jdbcClient.sql(RECEPCION)
                .param("tenantId", tenantId)
                .param("recepcionId", recepcionId)
                .query((rs, rowNumber) -> mapRecepcion(rs))
                .optional()
                .map(recepcion -> recepcion.conLineas(lineas(tenantId, recepcionId)));
    }

    private List<LineaRecepcionResult> lineas(UUID tenantId, UUID recepcionId) {
        return jdbcClient.sql(RECEPCION_LINEAS)
                .param("tenantId", tenantId)
                .param("recepcionId", recepcionId)
                .query((rs, rowNumber) -> mapLineaRecepcion(rs))
                .list();
    }

    private static OrdenCompraResumenResult mapResumen(ResultSet rs) throws SQLException {
        return new OrdenCompraResumenResult(
                JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("numero"), JdbcColumns.uuid(rs, "proveedor_uuid"),
                rs.getString("razon_social"), JdbcColumns.uuid(rs, "establecimiento_uuid"),
                JdbcColumns.date(rs, "fecha_emision"), JdbcColumns.date(rs, "fecha_entrega_estimada"),
                rs.getString("moneda"), rs.getBigDecimal("total"), rs.getString("estado"));
    }

    private static RecepcionResult mapRecepcion(ResultSet rs) throws SQLException {
        return new RecepcionResult(
                JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("numero"), JdbcColumns.uuid(rs, "orden_uuid"),
                JdbcColumns.uuid(rs, "proveedor_uuid"), JdbcColumns.uuid(rs, "establecimiento_uuid"),
                JdbcColumns.uuid(rs, "almacen_uuid"), rs.getString("documento_proveedor_tipo"),
                rs.getString("documento_proveedor_serie"), rs.getString("documento_proveedor_numero"),
                rs.getString("guia_remision_remitente"), rs.getString("guia_remision_transportista"),
                JdbcColumns.instant(rs, "fecha_recepcion"), rs.getBigDecimal("temperatura_recepcion_c"),
                rs.getBigDecimal("humedad_relativa_pct"), rs.getString("estado"), rs.getString("observacion"),
                List.of());
    }

    private static LineaRecepcionResult mapLineaRecepcion(ResultSet rs) throws SQLException {
        return new LineaRecepcionResult(
                JdbcColumns.uuid(rs, "uuid_publico"), rs.getInt("numero_linea"), rs.getInt("numero_linea_orden"),
                JdbcColumns.uuid(rs, "sku_uuid"), rs.getString("numero_lote"),
                JdbcColumns.date(rs, "fecha_fabricacion"), JdbcColumns.date(rs, "fecha_vencimiento"),
                rs.getBigDecimal("cantidad_recibida"), rs.getBigDecimal("cantidad_aceptada"),
                rs.getBigDecimal("cantidad_rechazada"), rs.getBigDecimal("costo_unitario"),
                rs.getString("decision_calidad"), rs.getString("motivo_decision"), rs.getString("observacion"),
                JdbcColumns.uuid(rs, "lote_uuid"));
    }
}
