package com.softprimesolutions.ventas.infrastructure.persistence.write.adapter;

import static com.softprimesolutions.ventas.infrastructure.persistence.JdbcEscrituras.exigirUnaFila;
import static com.softprimesolutions.ventas.infrastructure.persistence.JdbcEscrituras.guardarUnico;

import com.softprimesolutions.ventas.application.port.out.GuardadoOutcome;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.domain.model.LineaVenta;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.domain.model.Venta;
import com.softprimesolutions.ventas.infrastructure.persistence.JdbcColumns;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

@Repository
public class VentaJdbcWriteAdapter implements VentaWritePort {

    private static final String UK_IDEMPOTENCIA = "uk_venta_idempotency";
    private static final String POR_IDEMPOTENCIA = """
            SELECT v.uuid_publico, v.huella_solicitud
              FROM sch_venta.venta v
              JOIN sch_admin.tenant t ON t.id = v.tenant_id
             WHERE t.uuid_publico = :tenantId AND v.idempotency_key = :idempotencyKey AND v.es_activo = '1'
            """;
    private static final String INSERTAR_VENTA = """
            INSERT INTO sch_venta.venta
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, terminal_id, turno_caja_id,
                 vendedor_usuario_id, numero_operacion, idempotency_key, huella_solicitud, fecha_venta, moneda,
                 subtotal, descuento_total, impuesto_total, redondeo, total, estado, created_at, created_by)
            SELECT :ventaId, tc.tenant_id, tc.empresa_id, tc.establecimiento_id, tc.terminal_id, tc.id,
                   (SELECT m.id FROM sch_seguridad.membership m
                     WHERE m.uuid_publico = :vendedorId AND m.tenant_id = tc.tenant_id),
                   :numero, :idempotencyKey, :huella, :fecha, 'PEN', :subtotal, 0, 0, 0, :total, 'CONFIRMADA',
                   :fecha, :actor
              FROM sch_venta.turno_caja tc
              JOIN sch_admin.tenant t ON t.id = tc.tenant_id
             WHERE t.uuid_publico = :tenantId AND tc.uuid_publico = :turnoId AND tc.estado = 'ABIERTO'
            """;
    private static final String INSERTAR_LINEA = """
            INSERT INTO sch_venta.venta_linea
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, venta_id, numero_linea, sku_id,
                 descripcion_snapshot, unidad_venta_codigo, es_fraccion, cantidad, precio_unitario, descuento,
                 impuesto, total_linea, created_at, created_by)
            SELECT :lineaId, v.tenant_id, v.empresa_id, v.establecimiento_id, v.id, :numeroLinea, k.id,
                   :descripcion, :unidad, :esFraccion, :cantidad, :precio, 0, 0, :totalLinea, :fecha, :actor
              FROM sch_venta.venta v
              JOIN sch_catalogo.sku_comercial k ON k.tenant_id = v.tenant_id AND k.uuid_publico = :skuId
             WHERE v.uuid_publico = :ventaId
            """;
    private static final String ASEGURAR_EFECTIVO = """
            INSERT INTO sch_venta.medio_pago
                (tenant_id, codigo, nombre, tipo, requiere_referencia, permite_vuelto, created_by)
            SELECT t.id, 'EFECTIVO', 'Efectivo', 'EFECTIVO', FALSE, TRUE, :actor
              FROM sch_admin.tenant t
             WHERE t.uuid_publico = :tenantId
            ON CONFLICT (tenant_id, codigo) WHERE es_activo = '1' DO NOTHING
            """;
    private static final String INSERTAR_PAGO = """
            INSERT INTO sch_venta.pago_venta
                (tenant_id, empresa_id, establecimiento_id, venta_id, medio_pago_id, monto, monto_recibido, vuelto,
                 moneda, estado, pagado_at)
            SELECT v.tenant_id, v.empresa_id, v.establecimiento_id, v.id,
                   (SELECT mp.id FROM sch_venta.medio_pago mp
                     WHERE mp.tenant_id = v.tenant_id AND mp.codigo = 'EFECTIVO' AND mp.es_activo = '1'),
                   :monto, :montoRecibido, :vuelto, 'PEN', 'CONFIRMADO', :fecha
              FROM sch_venta.venta v
             WHERE v.uuid_publico = :ventaId
            """;
    private static final String INSERTAR_LOTE = """
            INSERT INTO sch_venta.venta_linea_lote (tenant_id, venta_linea_id, lote_id, cantidad)
            SELECT vl.tenant_id, vl.id, l.id, :cantidad
              FROM sch_venta.venta_linea vl
              JOIN sch_inventario.lote l ON l.tenant_id = vl.tenant_id AND l.uuid_publico = :loteId
              JOIN sch_admin.tenant t ON t.id = vl.tenant_id
             WHERE t.uuid_publico = :tenantId AND vl.uuid_publico = :lineaId
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transaction;

    public VentaJdbcWriteAdapter(JdbcClient jdbcClient, TransactionOperations transaction) {
        this.jdbcClient = jdbcClient;
        this.transaction = transaction;
    }

    @Override
    public Optional<VentaExistente> findPorIdempotencia(UUID tenantId, String idempotencyKey) {
        return jdbcClient.sql(POR_IDEMPOTENCIA)
                .param("tenantId", tenantId)
                .param("idempotencyKey", idempotencyKey)
                .query((rs, rowNumber) -> new VentaExistente(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("huella_solicitud")))
                .optional();
    }

    @Override
    public GuardadoOutcome insertar(Venta venta, String idempotencyKey, String huella) {
        return guardarUnico(UK_IDEMPOTENCIA, () -> transaction.executeWithoutResult(
                status -> persistir(venta, idempotencyKey, huella)));
    }

    @Override
    public void registrarLotes(UUID tenantId, UUID ventaLineaId, List<LoteConsumo> lotes) {
        lotes.forEach(lote -> exigirUnaFila(INSERTAR_LOTE, jdbcClient.sql(INSERTAR_LOTE)
                .param("tenantId", tenantId)
                .param("lineaId", ventaLineaId)
                .param("loteId", lote.loteId())
                .param("cantidad", lote.cantidad())
                .update()));
    }

    private void persistir(Venta venta, String idempotencyKey, String huella) {
        exigirUnaFila(INSERTAR_VENTA, insertarVenta(venta, idempotencyKey, huella));
        venta.lineas().forEach(linea -> exigirUnaFila(INSERTAR_LINEA, insertarLinea(venta, linea)));
        asegurarEfectivo(venta);
        exigirUnaFila(INSERTAR_PAGO, insertarPago(venta));
    }

    private int insertarVenta(Venta venta, String idempotencyKey, String huella) {
        return jdbcClient.sql(INSERTAR_VENTA)
                .param("ventaId", venta.id())
                .param("tenantId", venta.tenantId())
                .param("turnoId", venta.turnoId())
                .param("vendedorId", venta.vendedor().id())
                .param("numero", venta.numeroOperacion())
                .param("idempotencyKey", idempotencyKey)
                .param("huella", huella)
                .param("fecha", JdbcColumns.offset(venta.fechaVenta()))
                .param("subtotal", venta.subtotal())
                .param("total", venta.total())
                .param("actor", venta.vendedor().codigo())
                .update();
    }

    private int insertarLinea(Venta venta, LineaVenta linea) {
        return jdbcClient.sql(INSERTAR_LINEA)
                .param("lineaId", linea.id())
                .param("ventaId", venta.id())
                .param("numeroLinea", linea.numeroLinea())
                .param("skuId", linea.skuId())
                .param("descripcion", linea.descripcion())
                .param("unidad", linea.unidadVentaCodigo())
                .param("esFraccion", linea.esFraccion())
                .param("cantidad", linea.cantidad())
                .param("precio", linea.precioUnitario())
                .param("totalLinea", linea.totalLinea())
                .param("fecha", JdbcColumns.offset(venta.fechaVenta()))
                .param("actor", venta.vendedor().codigo())
                .update();
    }

    private void asegurarEfectivo(Venta venta) {
        jdbcClient.sql(ASEGURAR_EFECTIVO)
                .param("tenantId", venta.tenantId())
                .param("actor", venta.vendedor().codigo())
                .update();
    }

    private int insertarPago(Venta venta) {
        return jdbcClient.sql(INSERTAR_PAGO)
                .param("ventaId", venta.id())
                .param("monto", venta.pago().monto())
                .param("montoRecibido", venta.pago().montoRecibido())
                .param("vuelto", venta.pago().vuelto())
                .param("fecha", JdbcColumns.offset(venta.fechaVenta()))
                .update();
    }
}
