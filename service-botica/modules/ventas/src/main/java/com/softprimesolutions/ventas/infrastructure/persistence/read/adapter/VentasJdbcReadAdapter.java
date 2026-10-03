package com.softprimesolutions.ventas.infrastructure.persistence.read.adapter;

import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.result.LineaVentaResult;
import com.softprimesolutions.ventas.application.dto.result.LoteConsumidoResult;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.PagoResult;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import com.softprimesolutions.ventas.application.mapper.VentasApplicationMapper;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import com.softprimesolutions.ventas.infrastructure.persistence.TurnoRows;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class VentasJdbcReadAdapter implements VentasReadPort {

    private static final String CABECERA = """
            SELECT v.uuid_publico, v.numero_operacion, tp.uuid_publico AS terminal_uuid,
                   tc.uuid_publico AS turno_uuid, es.uuid_publico AS establecimiento_uuid,
                   m.uuid_publico AS vendedor_uuid, v.fecha_venta, v.moneda, v.subtotal, v.descuento_total,
                   v.impuesto_total, v.total, v.estado
              FROM sch_venta.venta v
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_organizacion.terminal_pos tp ON tp.id = v.terminal_id AND tp.tenant_id = v.tenant_id
              JOIN sch_venta.turno_caja tc ON tc.id = v.turno_caja_id AND tc.tenant_id = v.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = v.establecimiento_id AND es.tenant_id = v.tenant_id
              JOIN sch_seguridad.membership m ON m.id = v.vendedor_usuario_id
             WHERE t.uuid_publico = :tenantId AND v.es_activo = '1' AND v.uuid_publico = :ventaId
            """;
    private static final String LINEAS = """
            SELECT vl.uuid_publico, vl.numero_linea, k.uuid_publico AS sku_uuid, vl.descripcion_snapshot,
                   vl.unidad_venta_codigo, vl.cantidad, vl.precio_unitario, vl.total_linea
              FROM sch_venta.venta_linea vl
              JOIN sch_venta.venta v ON v.tenant_id = vl.tenant_id AND v.id = vl.venta_id
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.tenant_id = vl.tenant_id AND k.id = vl.sku_id
             WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId AND vl.es_activo = '1'
             ORDER BY vl.numero_linea
            """;
    private static final String LOTES = """
            SELECT vl.uuid_publico AS linea_uuid, l.uuid_publico AS lote_uuid, vll.cantidad
              FROM sch_venta.venta_linea_lote vll
              JOIN sch_venta.venta_linea vl ON vl.tenant_id = vll.tenant_id AND vl.id = vll.venta_linea_id
              JOIN sch_venta.venta v ON v.tenant_id = vl.tenant_id AND v.id = vl.venta_id
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_inventario.lote l ON l.tenant_id = vll.tenant_id AND l.id = vll.lote_id
             WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId
             ORDER BY vl.numero_linea, l.fecha_vencimiento, l.numero_lote
            """;
    private static final String PAGO = """
            SELECT mp.codigo AS medio_codigo, p.monto, p.monto_recibido, p.vuelto
              FROM sch_venta.pago_venta p
              JOIN sch_venta.venta v ON v.tenant_id = p.tenant_id AND v.id = p.venta_id
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_venta.medio_pago mp ON mp.tenant_id = p.tenant_id AND mp.id = p.medio_pago_id
             WHERE t.uuid_publico = :tenantId AND v.uuid_publico = :ventaId AND p.es_activo = '1'
             ORDER BY p.id
            """;
    private static final String VENTAS_FROM = """
              FROM sch_venta.venta v
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
              JOIN sch_organizacion.terminal_pos tp ON tp.id = v.terminal_id AND tp.tenant_id = v.tenant_id
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.id = v.establecimiento_id AND es.tenant_id = v.tenant_id
             WHERE t.uuid_publico = :tenantId AND v.es_activo = '1'
               AND (CAST(:establecimientoId AS uuid) IS NULL OR es.uuid_publico = CAST(:establecimientoId AS uuid))
               AND (CAST(:desde AS timestamptz) IS NULL OR v.fecha_venta >= CAST(:desde AS timestamptz))
               AND (CAST(:hasta AS timestamptz) IS NULL OR v.fecha_venta < CAST(:hasta AS timestamptz))
            """;
    private static final String VENTAS_CONTEO = "SELECT COUNT(*)" + VENTAS_FROM;
    private static final String VENTAS_PAGINA = """
            SELECT v.uuid_publico, v.numero_operacion, tp.uuid_publico AS terminal_uuid, v.fecha_venta, v.total,
                   v.estado
            """ + VENTAS_FROM + " ORDER BY v.fecha_venta DESC, v.id DESC LIMIT :limit OFFSET :offset";

    private final JdbcClient jdbcClient;

    public VentasJdbcReadAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoResult> findTurno(UUID tenantId, UUID turnoId) {
        return jdbcClient.sql(TurnoRows.POR_ID)
                .param("tenantId", tenantId)
                .param("turnoId", turnoId)
                .query((rs, rowNumber) -> VentasApplicationMapper.toResult(TurnoRows.map(rs, tenantId)))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<TurnoResult> findTurnoAbierto(UUID tenantId, UUID terminalId) {
        return jdbcClient.sql(TurnoRows.ABIERTO_POR_TERMINAL)
                .param("tenantId", tenantId)
                .param("terminalId", terminalId)
                .query((rs, rowNumber) -> VentasApplicationMapper.toResult(TurnoRows.map(rs, tenantId)))
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<VentaResult> findVenta(UUID tenantId, UUID ventaId) {
        return jdbcClient.sql(CABECERA)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> new Cabecera(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("numero_operacion"),
                        JdbcColumns.uuid(rs, "terminal_uuid"), JdbcColumns.uuid(rs, "turno_uuid"),
                        JdbcColumns.uuid(rs, "establecimiento_uuid"), JdbcColumns.uuid(rs, "vendedor_uuid"),
                        JdbcColumns.instant(rs, "fecha_venta"), rs.getString("moneda"),
                        rs.getBigDecimal("subtotal"), rs.getBigDecimal("descuento_total"),
                        rs.getBigDecimal("impuesto_total"), rs.getBigDecimal("total"), rs.getString("estado")))
                .optional()
                .map(cabecera -> armar(tenantId, ventaId, cabecera));
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<VentaResumenResult> listarVentas(ListarVentasQuery query) {
        var total = jdbcClient.sql(VENTAS_CONTEO)
                .param("tenantId", query.tenantId())
                .param("establecimientoId", query.establecimientoId())
                .param("desde", JdbcColumns.offset(query.desde()))
                .param("hasta", JdbcColumns.offset(query.hasta()))
                .query(Long.class)
                .single();
        var items = jdbcClient.sql(VENTAS_PAGINA)
                .param("tenantId", query.tenantId())
                .param("establecimientoId", query.establecimientoId())
                .param("desde", JdbcColumns.offset(query.desde()))
                .param("hasta", JdbcColumns.offset(query.hasta()))
                .param("limit", query.size())
                .param("offset", query.page() * query.size())
                .query((rs, rowNumber) -> new VentaResumenResult(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("numero_operacion"),
                        JdbcColumns.uuid(rs, "terminal_uuid"), JdbcColumns.instant(rs, "fecha_venta"),
                        rs.getBigDecimal("total"), rs.getString("estado")))
                .list();
        return new PaginaResult<>(items, query.page(), query.size(), total);
    }

    private VentaResult armar(UUID tenantId, UUID ventaId, Cabecera cabecera) {
        var lotes = jdbcClient.sql(LOTES)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> Map.entry(
                        JdbcColumns.uuid(rs, "linea_uuid"),
                        new LoteConsumidoResult(JdbcColumns.uuid(rs, "lote_uuid"), rs.getBigDecimal("cantidad"))))
                .list()
                .stream()
                .collect(Collectors.groupingBy(
                        Map.Entry::getKey, Collectors.mapping(Map.Entry::getValue, Collectors.toList())));
        var lineas = jdbcClient.sql(LINEAS)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> {
                    var lineaId = JdbcColumns.uuid(rs, "uuid_publico");
                    return new LineaVentaResult(
                            rs.getInt("numero_linea"), JdbcColumns.uuid(rs, "sku_uuid"),
                            rs.getString("descripcion_snapshot"), rs.getString("unidad_venta_codigo"),
                            rs.getBigDecimal("cantidad"), rs.getBigDecimal("precio_unitario"),
                            rs.getBigDecimal("total_linea"), lotes.getOrDefault(lineaId, List.of()));
                })
                .list();
        var pago = jdbcClient.sql(PAGO)
                .param("tenantId", tenantId)
                .param("ventaId", ventaId)
                .query((rs, rowNumber) -> new PagoResult(
                        rs.getString("medio_codigo"), rs.getBigDecimal("monto"), rs.getBigDecimal("monto_recibido"),
                        rs.getBigDecimal("vuelto")))
                .list()
                .stream()
                .findFirst()
                .orElse(null);
        return new VentaResult(
                cabecera.id(), cabecera.numeroOperacion(), cabecera.terminalId(), cabecera.turnoId(),
                cabecera.establecimientoId(), cabecera.vendedorId(), cabecera.fechaVenta(), cabecera.moneda(),
                cabecera.subtotal(), cabecera.descuentoTotal(), cabecera.impuestoTotal(), cabecera.total(),
                cabecera.estado(), lineas, pago, null);
    }

    private record Cabecera(
            UUID id, String numeroOperacion, UUID terminalId, UUID turnoId, UUID establecimientoId, UUID vendedorId,
            Instant fechaVenta, String moneda, BigDecimal subtotal, BigDecimal descuentoTotal,
            BigDecimal impuestoTotal, BigDecimal total, String estado) {
    }
}
