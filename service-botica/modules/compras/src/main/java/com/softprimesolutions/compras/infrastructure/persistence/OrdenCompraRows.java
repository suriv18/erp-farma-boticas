package com.softprimesolutions.compras.infrastructure.persistence;

import com.softprimesolutions.compras.domain.model.CondicionesOrden;
import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.compras.domain.model.LineaOrdenCompra;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;

public final class OrdenCompraRows {

    private static final String HEADER = """
            SELECT o.uuid_publico, e.uuid_publico AS empresa_uuid, pr.uuid_publico AS proveedor_uuid,
                   es.uuid_publico AS establecimiento_uuid, o.numero, o.fecha_emision, o.fecha_entrega_estimada,
                   o.moneda, o.tipo_cambio, o.condicion_pago, o.dias_credito, o.subtotal, o.descuento_total,
                   o.impuesto_total, o.total, o.estado, o.observacion, m.uuid_publico AS aprobado_por,
                   o.aprobado_at, o.created_at, o.created_by, o.updated_at, o.updated_by
              FROM sch_abastecimiento.orden_compra o
              JOIN sch_admin.tenant t ON t.id = o.tenant_id
              JOIN sch_organizacion.empresa_operadora e ON e.id = o.empresa_id AND e.tenant_id = o.tenant_id
              JOIN sch_abastecimiento.proveedor pr ON pr.id = o.proveedor_id AND pr.tenant_id = o.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = o.establecimiento_destino_id AND es.tenant_id = o.tenant_id
              LEFT JOIN sch_seguridad.membership m ON m.id = o.aprobado_por_usuario_id
             WHERE t.uuid_publico = :tenantId AND o.uuid_publico = :ordenId AND o.es_activo = '1'
            """;
    private static final String BLOQUEO = " FOR UPDATE OF o";
    private static final String LINEAS = """
            SELECT l.numero_linea, k.uuid_publico AS sku_uuid, l.descripcion_snapshot, l.cantidad,
                   l.unidad_medida_codigo, l.precio_unitario, l.descuento, l.impuesto, l.total_linea,
                   l.tolerancia_exceso_pct, l.tolerancia_defecto_pct,
                   COALESCE((SELECT SUM(rl.cantidad_aceptada)
                               FROM sch_abastecimiento.recepcion_compra_linea rl
                               JOIN sch_abastecimiento.recepcion_compra r
                                 ON r.id = rl.recepcion_id AND r.tenant_id = rl.tenant_id
                              WHERE rl.orden_compra_linea_id = l.id AND rl.es_activo = '1'
                                AND r.estado = 'CONFIRMADA' AND r.es_activo = '1'), 0) AS cantidad_recibida
              FROM sch_abastecimiento.orden_compra_linea l
              JOIN sch_abastecimiento.orden_compra o ON o.id = l.orden_compra_id AND o.tenant_id = l.tenant_id
              JOIN sch_admin.tenant t ON t.id = o.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.id = l.sku_id AND k.tenant_id = l.tenant_id
             WHERE t.uuid_publico = :tenantId AND o.uuid_publico = :ordenId AND l.es_activo = '1'
             ORDER BY l.numero_linea
            """;

    private OrdenCompraRows() {
    }

    public static Optional<OrdenCompra> buscar(
            JdbcClient jdbcClient, UUID tenantId, UUID ordenId, boolean paraActualizar) {
        var encabezado = jdbcClient.sql(paraActualizar ? HEADER + BLOQUEO : HEADER)
                .param("tenantId", tenantId)
                .param("ordenId", ordenId)
                .query((rs, rowNumber) -> mapEncabezado(rs, tenantId))
                .optional();
        return encabezado.map(builder -> {
            builder.lineas = jdbcClient.sql(LINEAS)
                    .param("tenantId", tenantId)
                    .param("ordenId", ordenId)
                    .query((rs, rowNumber) -> mapLinea(rs))
                    .list();
            return OrdenCompra.restore(builder);
        });
    }

    private static OrdenCompra.Builder mapEncabezado(ResultSet rs, UUID tenantId) throws SQLException {
        var builder = new OrdenCompra.Builder();
        builder.id = JdbcColumns.uuid(rs, "uuid_publico");
        builder.tenantId = tenantId;
        builder.empresaId = JdbcColumns.uuid(rs, "empresa_uuid");
        builder.proveedorId = JdbcColumns.uuid(rs, "proveedor_uuid");
        builder.establecimientoDestinoId = JdbcColumns.uuid(rs, "establecimiento_uuid");
        builder.numero = rs.getString("numero");
        builder.fechaEmision = JdbcColumns.date(rs, "fecha_emision");
        builder.condiciones = new CondicionesOrden(
                JdbcColumns.date(rs, "fecha_entrega_estimada"), rs.getString("moneda"),
                rs.getBigDecimal("tipo_cambio"), rs.getString("condicion_pago"), rs.getInt("dias_credito"),
                rs.getString("observacion"));
        builder.subtotal = rs.getBigDecimal("subtotal");
        builder.descuentoTotal = rs.getBigDecimal("descuento_total");
        builder.impuestoTotal = rs.getBigDecimal("impuesto_total");
        builder.total = rs.getBigDecimal("total");
        builder.estado = EstadoOrdenCompra.valueOf(rs.getString("estado"));
        builder.aprobadoPor = Optional.ofNullable(JdbcColumns.uuid(rs, "aprobado_por")).map(UUID::toString).orElse(null);
        builder.aprobadoAt = JdbcColumns.instant(rs, "aprobado_at");
        builder.lineas = List.of();
        builder.createdAt = JdbcColumns.instant(rs, "created_at");
        builder.createdBy = rs.getString("created_by");
        builder.updatedAt = JdbcColumns.instant(rs, "updated_at");
        builder.updatedBy = rs.getString("updated_by");
        return builder;
    }

    private static LineaOrdenCompra mapLinea(ResultSet rs) throws SQLException {
        return new LineaOrdenCompra(
                rs.getInt("numero_linea"), JdbcColumns.uuid(rs, "sku_uuid"), rs.getString("descripcion_snapshot"),
                rs.getBigDecimal("cantidad"), rs.getString("unidad_medida_codigo"),
                rs.getBigDecimal("precio_unitario"), rs.getBigDecimal("descuento"), rs.getBigDecimal("impuesto"),
                rs.getBigDecimal("total_linea"), rs.getBigDecimal("tolerancia_exceso_pct"),
                rs.getBigDecimal("tolerancia_defecto_pct"), rs.getBigDecimal("cantidad_recibida"));
    }
}
