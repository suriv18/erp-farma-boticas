package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.domain.model.EstadoOrdenCompra;
import com.softprimesolutions.compras.domain.model.LineaOrdenCompra;
import com.softprimesolutions.compras.domain.model.OrdenCompra;
import com.softprimesolutions.compras.infrastructure.persistence.JdbcColumns;
import com.softprimesolutions.compras.infrastructure.persistence.OrdenCompraRows;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

@Repository
public class OrdenCompraJdbcWriteAdapter implements OrdenCompraWritePort {

    private static final String INSERTAR_ORDEN = """
            INSERT INTO sch_abastecimiento.orden_compra
                (uuid_publico, tenant_id, empresa_id, proveedor_id, establecimiento_destino_id, numero,
                 fecha_emision, fecha_entrega_estimada, moneda, tipo_cambio, condicion_pago, dias_credito,
                 subtotal, descuento_total, impuesto_total, total, estado, observacion, created_at, created_by)
            SELECT :ordenId, t.id, es.empresa_id, pr.id, es.id, :numero, :fechaEmision,
                   CAST(:fechaEntregaEstimada AS date), :moneda, :tipoCambio, :condicionPago, :diasCredito,
                   :subtotal, :descuentoTotal, :impuestoTotal, :total, :estado, :observacion, :createdAt,
                   :createdBy
              FROM sch_admin.tenant t
              JOIN sch_organizacion.establecimiento_farmaceutico es
                ON es.tenant_id = t.id AND es.uuid_publico = :establecimientoId
              JOIN sch_abastecimiento.proveedor pr ON pr.tenant_id = t.id AND pr.uuid_publico = :proveedorId
             WHERE t.uuid_publico = :tenantId
            """;
    private static final String INSERTAR_LINEA = """
            INSERT INTO sch_abastecimiento.orden_compra_linea
                (tenant_id, empresa_id, orden_compra_id, numero_linea, sku_id, descripcion_snapshot, cantidad,
                 unidad_medida_codigo, precio_unitario, descuento, impuesto, total_linea, tolerancia_exceso_pct,
                 tolerancia_defecto_pct, created_at, created_by)
            SELECT o.tenant_id, o.empresa_id, o.id, :numeroLinea, k.id, :descripcion, :cantidad, :unidad,
                   :precioUnitario, :descuento, :impuesto, :totalLinea, :toleranciaExceso, :toleranciaDefecto,
                   :createdAt, :createdBy
              FROM sch_abastecimiento.orden_compra o
              JOIN sch_catalogo.sku_comercial k ON k.tenant_id = o.tenant_id AND k.uuid_publico = :skuId
             WHERE o.uuid_publico = :ordenId
            """;
    private static final String ACTUALIZAR_ESTADO = """
            UPDATE sch_abastecimiento.orden_compra
               SET estado = :estado, observacion = :observacion,
                   aprobado_por_usuario_id = COALESCE(
                       (SELECT m.id FROM sch_seguridad.membership m WHERE m.uuid_publico = CAST(:aprobadoPor AS uuid)),
                       aprobado_por_usuario_id),
                   aprobado_at = CAST(:aprobadoAt AS timestamptz), updated_at = :updatedAt, updated_by = :updatedBy
             WHERE uuid_publico = :ordenId AND estado = :estadoPrevio AND es_activo = '1'
               AND tenant_id = (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transaction;

    public OrdenCompraJdbcWriteAdapter(JdbcClient jdbcClient, TransactionOperations transaction) {
        this.jdbcClient = jdbcClient;
        this.transaction = transaction;
    }

    @Override
    public Optional<OrdenCompra> findById(UUID tenantId, UUID ordenId) {
        return OrdenCompraRows.buscar(jdbcClient, tenantId, ordenId, false);
    }

    @Override
    public Optional<OrdenCompra> findByIdParaActualizar(UUID tenantId, UUID ordenId) {
        return OrdenCompraRows.buscar(jdbcClient, tenantId, ordenId, true);
    }

    @Override
    public GuardadoOutcome insertar(OrdenCompra orden) {
        try {
            transaction.executeWithoutResult(status -> persistir(orden));
            return GuardadoOutcome.GUARDADO;
        } catch (DataIntegrityViolationException | FilaNoInsertada exception) {
            return GuardadoOutcome.DUPLICADO;
        }
    }

    @Override
    public boolean actualizarEstado(OrdenCompra orden, EstadoOrdenCompra estadoPrevio) {
        return jdbcClient.sql(ACTUALIZAR_ESTADO)
                .param("estado", orden.estado().name())
                .param("observacion", orden.observacion())
                .param("aprobadoPor", orden.aprobadoPor())
                .param("aprobadoAt", JdbcColumns.offset(orden.aprobadoAt()))
                .param("updatedAt", JdbcColumns.offset(orden.updatedAt()))
                .param("updatedBy", orden.updatedBy())
                .param("ordenId", orden.id())
                .param("estadoPrevio", estadoPrevio.name())
                .param("tenantId", orden.tenantId())
                .update() == 1;
    }

    private void persistir(OrdenCompra orden) {
        exigirUnaFila(insertarOrden(orden));
        orden.lineas().forEach(linea -> exigirUnaFila(insertarLinea(orden, linea)));
    }

    private int insertarOrden(OrdenCompra orden) {
        var condiciones = orden.condiciones();
        return jdbcClient.sql(INSERTAR_ORDEN)
                .param("ordenId", orden.id())
                .param("tenantId", orden.tenantId())
                .param("establecimientoId", orden.establecimientoDestinoId())
                .param("proveedorId", orden.proveedorId())
                .param("numero", orden.numero())
                .param("fechaEmision", orden.fechaEmision())
                .param("fechaEntregaEstimada", condiciones.fechaEntregaEstimada())
                .param("moneda", condiciones.moneda())
                .param("tipoCambio", condiciones.tipoCambio())
                .param("condicionPago", condiciones.condicionPago())
                .param("diasCredito", condiciones.diasCredito())
                .param("subtotal", orden.subtotal())
                .param("descuentoTotal", orden.descuentoTotal())
                .param("impuestoTotal", orden.impuestoTotal())
                .param("total", orden.total())
                .param("estado", orden.estado().name())
                .param("observacion", condiciones.observacion())
                .param("createdAt", JdbcColumns.offset(orden.createdAt()))
                .param("createdBy", orden.createdBy())
                .update();
    }

    private int insertarLinea(OrdenCompra orden, LineaOrdenCompra linea) {
        return jdbcClient.sql(INSERTAR_LINEA)
                .param("ordenId", orden.id())
                .param("numeroLinea", linea.numeroLinea())
                .param("skuId", linea.skuId())
                .param("descripcion", linea.descripcionSnapshot())
                .param("cantidad", linea.cantidad())
                .param("unidad", linea.unidadMedidaCodigo())
                .param("precioUnitario", linea.precioUnitario())
                .param("descuento", linea.descuento())
                .param("impuesto", linea.impuesto())
                .param("totalLinea", linea.totalLinea())
                .param("toleranciaExceso", linea.toleranciaExcesoPct())
                .param("toleranciaDefecto", linea.toleranciaDefectoPct())
                .param("createdAt", JdbcColumns.offset(orden.createdAt()))
                .param("createdBy", orden.createdBy())
                .update();
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) throw new FilaNoInsertada();
    }

    private static final class FilaNoInsertada extends RuntimeException {

        private static final long serialVersionUID = 1L;

        FilaNoInsertada() {
            super(null, null, false, false);
        }
    }
}
