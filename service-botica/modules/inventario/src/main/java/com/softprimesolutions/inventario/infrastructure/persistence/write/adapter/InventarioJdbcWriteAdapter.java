package com.softprimesolutions.inventario.infrastructure.persistence.write.adapter;

import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.MovimientoRegistrado;
import com.softprimesolutions.inventario.application.port.out.RegistroMovimiento;
import com.softprimesolutions.inventario.domain.model.EstadoLote;
import com.softprimesolutions.inventario.domain.model.Lote;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import com.softprimesolutions.inventario.domain.valueobject.LoteId;
import com.softprimesolutions.inventario.domain.valueobject.PosicionId;
import com.softprimesolutions.inventario.domain.valueobject.TenantId;
import com.softprimesolutions.inventario.infrastructure.persistence.JdbcColumns;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

@Repository
public class InventarioJdbcWriteAdapter implements InventarioWritePort {

    private static final String DOCUMENTO_AJUSTE_MANUAL = "AJUSTE_MANUAL";

    private static final String LOTE_SELECT = """
            SELECT l.uuid_publico, k.uuid_publico AS sku_uuid, l.numero_lote, l.fecha_vencimiento,
                   l.estado_lote, l.motivo_estado, l.bloqueado_at, l.bloqueado_por, l.created_at,
                   l.updated_at, l.updated_by
              FROM sch_inventario.lote l
              JOIN sch_admin.tenant t ON t.id = l.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.id = l.sku_id AND k.tenant_id = l.tenant_id
             WHERE t.uuid_publico = :tenantId AND l.es_activo = '1'
            """;
    private static final String LOTE_POR_ID = LOTE_SELECT + " AND l.uuid_publico = :loteId";
    private static final String LOTE_POR_CLAVE = LOTE_SELECT + """
               AND k.uuid_publico = :skuId AND l.numero_lote = :numeroLote
               AND l.fecha_vencimiento = :fechaVencimiento
            """;
    private static final String POSICION_DISPONIBLE = """
            SELECT p.uuid_publico, l.uuid_publico AS lote_uuid, a.uuid_publico AS almacen_uuid,
                   k.uuid_publico AS sku_uuid, p.cantidad_fisica, p.cantidad_reservada, p.version_lock
              FROM sch_inventario.posicion_inventario p
              JOIN sch_admin.tenant t ON t.id = p.tenant_id
              JOIN sch_organizacion.almacen a ON a.id = p.almacen_id AND a.tenant_id = p.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.id = p.sku_id AND k.tenant_id = p.tenant_id
              JOIN sch_inventario.lote l ON l.id = p.lote_id AND l.tenant_id = p.tenant_id
             WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :almacenId AND l.uuid_publico = :loteId
               AND p.estado_inventario = 'DISPONIBLE' AND p.ubicacion_id IS NULL AND p.es_activo = '1'
            """;
    private static final String ACTUALIZAR_ESTADO_LOTE = """
            UPDATE sch_inventario.lote
               SET estado_lote = :estado, motivo_estado = :motivo, bloqueado_at = :bloqueadoAt,
                   bloqueado_por = :bloqueadoPor, updated_at = :updatedAt, updated_by = :updatedBy
             WHERE uuid_publico = :loteId AND estado_lote = :estadoPrevio
               AND tenant_id = (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)
            """;
    private static final String MOVIMIENTO_POR_BUSINESS_UUID = """
            SELECT m.uuid_publico, p.uuid_publico AS posicion_uuid, l.uuid_publico AS lote_uuid,
                   m.tipo_movimiento, m.naturaleza, m.cantidad, m.stock_anterior, m.stock_posterior,
                   m.fecha_negocio, m.metadata ->> 'huellaSolicitud' AS huella
              FROM sch_inventario.movimiento_inventario m
              JOIN sch_admin.tenant t ON t.id = m.tenant_id
              JOIN sch_inventario.lote l ON l.id = m.lote_id AND l.tenant_id = m.tenant_id
              LEFT JOIN sch_inventario.posicion_inventario p
                ON p.tenant_id = m.tenant_id AND p.lote_id = m.lote_id
               AND p.almacen_id = COALESCE(m.almacen_origen_id, m.almacen_destino_id)
               AND p.estado_inventario = 'DISPONIBLE' AND p.ubicacion_id IS NULL AND p.es_activo = '1'
             WHERE t.uuid_publico = :tenantId AND m.business_uuid = :businessUuid
            """;
    private static final String INSERTAR_LOTE = """
            INSERT INTO sch_inventario.lote
                (uuid_publico, tenant_id, sku_id, proveedor_id, numero_lote, fecha_vencimiento,
                 origen_recepcion_linea_id, estado_lote, created_at, created_by)
            SELECT :loteId, t.id, k.id,
                   (SELECT p.id FROM sch_abastecimiento.proveedor p
                     WHERE p.tenant_id = t.id AND p.uuid_publico = CAST(:proveedorId AS uuid)),
                   :numeroLote, :fechaVencimiento,
                   (SELECT rl.id FROM sch_abastecimiento.recepcion_compra_linea rl
                     WHERE rl.tenant_id = t.id AND rl.uuid_publico = CAST(:origenLineaId AS uuid)),
                   'HABILITADO', :fecha, :actor
              FROM sch_admin.tenant t
              JOIN sch_catalogo.sku_comercial k ON k.tenant_id = t.id AND k.uuid_publico = :skuId
             WHERE t.uuid_publico = :tenantId
            """;
    private static final String INSERTAR_POSICION = """
            INSERT INTO sch_inventario.posicion_inventario
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, almacen_id, sku_id, lote_id,
                 estado_inventario, cantidad_fisica, ultimo_movimiento_at, created_at, created_by)
            SELECT :posicionId, a.tenant_id, a.empresa_id, a.establecimiento_id, a.id, k.id, l.id,
                   'DISPONIBLE', :cantidadFisica, :fecha, :fecha, :actor
              FROM sch_organizacion.almacen a
              JOIN sch_admin.tenant t ON t.id = a.tenant_id
              JOIN sch_catalogo.sku_comercial k ON k.tenant_id = a.tenant_id AND k.uuid_publico = :skuId
              JOIN sch_inventario.lote l ON l.tenant_id = a.tenant_id AND l.uuid_publico = :loteId
             WHERE t.uuid_publico = :tenantId AND a.uuid_publico = :almacenId
            """;
    private static final String ACTUALIZAR_POSICION = """
            UPDATE sch_inventario.posicion_inventario
               SET cantidad_fisica = :cantidadFisica, version_lock = version_lock + 1,
                   ultimo_movimiento_at = :fecha, updated_at = :fecha, updated_by = :actor
             WHERE uuid_publico = :posicionId AND version_lock = :version
               AND cantidad_reservada <= :cantidadFisica
            """;
    private static final String INSERTAR_MOVIMIENTO = """
            INSERT INTO sch_inventario.movimiento_inventario
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, sku_id, lote_id, tipo_movimiento,
                 tipo_operacion_sunat, naturaleza, cantidad, almacen_origen_id, almacen_destino_id,
                 stock_anterior, stock_posterior, documento_tipo, documento_uuid, fecha_negocio, actor,
                 observacion, business_uuid, metadata)
            SELECT :movimientoId, p.tenant_id, p.empresa_id, p.establecimiento_id, p.sku_id, p.lote_id, :tipo,
                   :tipoOperacionSunat, CAST(:naturaleza AS char(1)), :cantidad,
                   CASE WHEN :ingreso THEN NULL ELSE p.almacen_id END,
                   CASE WHEN :ingreso THEN p.almacen_id ELSE NULL END,
                   :stockAnterior, :stockPosterior, :documentoTipo, CAST(:documentoUuid AS uuid), :fecha, :actor,
                   :motivo, CAST(:businessUuid AS uuid), CAST(:metadata AS jsonb)
              FROM sch_inventario.posicion_inventario p
             WHERE p.uuid_publico = :posicionId
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transaction;

    public InventarioJdbcWriteAdapter(JdbcClient jdbcClient, TransactionOperations transaction) {
        this.jdbcClient = jdbcClient;
        this.transaction = transaction;
    }

    @Override
    public Optional<Lote> findLote(UUID tenantId, UUID loteId) {
        return jdbcClient.sql(LOTE_POR_ID)
                .param("tenantId", tenantId)
                .param("loteId", loteId)
                .query((rs, rowNumber) -> mapLote(rs, tenantId))
                .optional();
    }

    @Override
    public Optional<Lote> findLotePorClave(
            UUID tenantId, UUID skuId, String numeroLote, LocalDate fechaVencimiento) {
        return jdbcClient.sql(LOTE_POR_CLAVE)
                .param("tenantId", tenantId)
                .param("skuId", skuId)
                .param("numeroLote", numeroLote)
                .param("fechaVencimiento", fechaVencimiento)
                .query((rs, rowNumber) -> mapLote(rs, tenantId))
                .optional();
    }

    @Override
    public Optional<PosicionInventario> findPosicion(UUID tenantId, UUID almacenId, UUID loteId) {
        return jdbcClient.sql(POSICION_DISPONIBLE)
                .param("tenantId", tenantId)
                .param("almacenId", almacenId)
                .param("loteId", loteId)
                .query((rs, rowNumber) -> mapPosicion(rs))
                .optional();
    }

    @Override
    public Optional<MovimientoRegistrado> findMovimientoPorBusinessUuid(UUID tenantId, UUID businessUuid) {
        return jdbcClient.sql(MOVIMIENTO_POR_BUSINESS_UUID)
                .param("tenantId", tenantId)
                .param("businessUuid", businessUuid)
                .query((rs, rowNumber) -> mapMovimiento(rs))
                .optional();
    }

    @Override
    public boolean actualizarEstado(Lote lote, EstadoLote estadoPrevio) {
        return jdbcClient.sql(ACTUALIZAR_ESTADO_LOTE)
                .param("estado", lote.estado().name())
                .param("motivo", lote.motivoEstado())
                .param("bloqueadoAt", JdbcColumns.offset(lote.bloqueadoAt()))
                .param("bloqueadoPor", lote.bloqueadoPor())
                .param("updatedAt", JdbcColumns.offset(lote.updatedAt()))
                .param("updatedBy", lote.updatedBy())
                .param("loteId", lote.id().value())
                .param("estadoPrevio", estadoPrevio.name())
                .param("tenantId", lote.tenantId().value())
                .update() == 1;
    }

    @Override
    public RegistroOutcome registrar(RegistroMovimiento registro) {
        try {
            transaction.executeWithoutResult(status -> persistir(registro));
            return RegistroOutcome.REGISTRADO;
        } catch (DataIntegrityViolationException | ConflictoConcurrencia exception) {
            return RegistroOutcome.MODIFICACION_CONCURRENTE;
        }
    }

    private void persistir(RegistroMovimiento registro) {
        if (registro.loteNuevo()) exigirUnaFila(insertarLote(registro));
        exigirUnaFila(registro.posicionNueva() ? insertarPosicion(registro) : actualizarPosicion(registro));
        exigirUnaFila(insertarMovimiento(registro));
    }

    private int insertarLote(RegistroMovimiento registro) {
        var lote = registro.lote();
        return jdbcClient.sql(INSERTAR_LOTE)
                .param("loteId", lote.id().value())
                .param("numeroLote", lote.numeroLote())
                .param("fechaVencimiento", lote.fechaVencimiento())
                .param("fecha", JdbcColumns.offset(registro.fechaNegocio()))
                .param("actor", registro.actor().codigo())
                .param("skuId", registro.skuId())
                .param("tenantId", registro.tenantId().value())
                .param("proveedorId", origen(registro, DocumentoOrigen::proveedorId))
                .param("origenLineaId", origen(registro, DocumentoOrigen::lineaId))
                .update();
    }

    private int insertarPosicion(RegistroMovimiento registro) {
        return jdbcClient.sql(INSERTAR_POSICION)
                .param("posicionId", registro.posicion().id().value())
                .param("cantidadFisica", registro.posicion().cantidadFisica())
                .param("fecha", JdbcColumns.offset(registro.fechaNegocio()))
                .param("actor", registro.actor().codigo())
                .param("skuId", registro.skuId())
                .param("loteId", registro.lote().id().value())
                .param("tenantId", registro.tenantId().value())
                .param("almacenId", registro.almacenId())
                .update();
    }

    private int actualizarPosicion(RegistroMovimiento registro) {
        return jdbcClient.sql(ACTUALIZAR_POSICION)
                .param("cantidadFisica", registro.posicion().cantidadFisica())
                .param("fecha", JdbcColumns.offset(registro.fechaNegocio()))
                .param("actor", registro.actor().codigo())
                .param("posicionId", registro.posicion().id().value())
                .param("version", registro.posicion().version())
                .update();
    }

    private int insertarMovimiento(RegistroMovimiento registro) {
        return jdbcClient.sql(INSERTAR_MOVIMIENTO)
                .param("movimientoId", registro.movimientoId())
                .param("tipo", registro.tipo().name())
                .param("tipoOperacionSunat", registro.tipo().tipoOperacionSunat())
                .param("documentoTipo", Optional.ofNullable(registro.origen())
                        .map(DocumentoOrigen::tipo).orElse(DOCUMENTO_AJUSTE_MANUAL))
                .param("documentoUuid", origen(registro, DocumentoOrigen::documentoId))
                .param("naturaleza", registro.tipo().naturaleza())
                .param("cantidad", registro.cantidad())
                .param("ingreso", registro.tipo().ingreso())
                .param("stockAnterior", registro.stockAnterior())
                .param("stockPosterior", registro.stockPosterior())
                .param("fecha", JdbcColumns.offset(registro.fechaNegocio()))
                .param("actor", registro.actor().codigo())
                .param("motivo", registro.motivo())
                .param("businessUuid", registro.businessUuid())
                .param("metadata", "{\"huellaSolicitud\":\"" + registro.huella() + "\"}")
                .param("posicionId", registro.posicion().id().value())
                .update();
    }

    private static UUID origen(RegistroMovimiento registro, Function<DocumentoOrigen, UUID> campo) {
        return Optional.ofNullable(registro.origen()).map(campo).orElse(null);
    }

    private static void exigirUnaFila(int filas) {
        if (filas != 1) throw new ConflictoConcurrencia();
    }

    private static Lote mapLote(ResultSet rs, UUID tenantId) throws SQLException {
        return Lote.restore(
                new LoteId(JdbcColumns.uuid(rs, "uuid_publico")), new TenantId(tenantId),
                JdbcColumns.uuid(rs, "sku_uuid"), rs.getString("numero_lote"),
                JdbcColumns.date(rs, "fecha_vencimiento"), EstadoLote.valueOf(rs.getString("estado_lote")),
                rs.getString("motivo_estado"), JdbcColumns.instant(rs, "bloqueado_at"),
                rs.getString("bloqueado_por"), JdbcColumns.instant(rs, "created_at"),
                JdbcColumns.instant(rs, "updated_at"), rs.getString("updated_by"));
    }

    private static MovimientoRegistrado mapMovimiento(ResultSet rs) throws SQLException {
        return new MovimientoRegistrado(
                new MovimientoResult(
                        JdbcColumns.uuid(rs, "uuid_publico"), JdbcColumns.uuid(rs, "posicion_uuid"),
                        JdbcColumns.uuid(rs, "lote_uuid"), rs.getString("tipo_movimiento"),
                        rs.getString("naturaleza"), rs.getBigDecimal("cantidad"),
                        rs.getBigDecimal("stock_anterior"), rs.getBigDecimal("stock_posterior"),
                        JdbcColumns.instant(rs, "fecha_negocio")),
                rs.getString("huella"));
    }

    private static PosicionInventario mapPosicion(ResultSet rs) throws SQLException {
        return PosicionInventario.restore(
                new PosicionId(JdbcColumns.uuid(rs, "uuid_publico")), JdbcColumns.uuid(rs, "lote_uuid"),
                JdbcColumns.uuid(rs, "almacen_uuid"), JdbcColumns.uuid(rs, "sku_uuid"),
                rs.getBigDecimal("cantidad_fisica"), rs.getBigDecimal("cantidad_reservada"),
                rs.getLong("version_lock"));
    }

    private static final class ConflictoConcurrencia extends RuntimeException {

        private static final long serialVersionUID = 1L;

        ConflictoConcurrencia() {
            super(null, null, false, false);
        }
    }
}
