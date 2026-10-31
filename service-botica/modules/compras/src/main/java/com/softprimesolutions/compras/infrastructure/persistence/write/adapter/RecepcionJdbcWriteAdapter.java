package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.RecepcionWritePort;
import com.softprimesolutions.compras.domain.model.LineaRecepcion;
import com.softprimesolutions.compras.domain.model.Recepcion;
import com.softprimesolutions.compras.infrastructure.persistence.JdbcColumns;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionOperations;

@Repository
public class RecepcionJdbcWriteAdapter implements RecepcionWritePort {

    private static final String POR_BUSINESS_UUID = """
            SELECT r.uuid_publico, r.huella_solicitud
              FROM sch_abastecimiento.recepcion_compra r
              JOIN sch_admin.tenant t ON t.id = r.tenant_id
             WHERE t.uuid_publico = :tenantId AND r.business_uuid = :businessUuid
            """;
    private static final String INSERTAR_RECEPCION = """
            INSERT INTO sch_abastecimiento.recepcion_compra
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, almacen_id, orden_compra_id,
                 proveedor_id, numero, guia_remision_remitente, guia_remision_transportista,
                 documento_proveedor_tipo, documento_proveedor_serie, documento_proveedor_numero,
                 fecha_recepcion, temperatura_recepcion_c, humedad_relativa_pct, recibido_por_usuario_id,
                 estado, observacion, created_at, created_by, business_uuid, huella_solicitud)
            SELECT :recepcionId, o.tenant_id, o.empresa_id, a.establecimiento_id, a.id, o.id, o.proveedor_id,
                   :numero, :guiaRemitente, :guiaTransportista, :documentoTipo, :documentoSerie,
                   :documentoNumero, :fechaRecepcion, CAST(:temperatura AS numeric), CAST(:humedad AS numeric),
                   (SELECT m.id FROM sch_seguridad.membership m WHERE m.uuid_publico = CAST(:recibidoPor AS uuid)),
                   'CONFIRMADA', :observacion, :fechaRecepcion, :recibidoPor, CAST(:businessUuid AS uuid),
                   :huella
              FROM sch_abastecimiento.orden_compra o
              JOIN sch_admin.tenant t ON t.id = o.tenant_id
              JOIN sch_organizacion.almacen a ON a.tenant_id = o.tenant_id AND a.uuid_publico = :almacenId
             WHERE t.uuid_publico = :tenantId AND o.uuid_publico = :ordenId
            """;
    private static final String INSERTAR_LINEA = """
            INSERT INTO sch_abastecimiento.recepcion_compra_linea
                (uuid_publico, tenant_id, empresa_id, establecimiento_id, recepcion_id, numero_linea,
                 orden_compra_linea_id, sku_id, numero_lote, fecha_fabricacion, fecha_vencimiento,
                 cantidad_recibida, cantidad_aceptada, cantidad_cuarentena, cantidad_rechazada, costo_unitario,
                 decision_calidad, motivo_decision, observacion, created_at, created_by)
            SELECT :lineaId, r.tenant_id, r.empresa_id, r.establecimiento_id, r.id, :numeroLinea, ol.id,
                   ol.sku_id, :numeroLote, CAST(:fechaFabricacion AS date), :fechaVencimiento, :recibida,
                   :aceptada, 0, :rechazada, CAST(:costoUnitario AS numeric), :decision, :motivo, :observacion,
                   :fechaRecepcion, :recibidoPor
              FROM sch_abastecimiento.recepcion_compra r
              JOIN sch_abastecimiento.orden_compra_linea ol
                ON ol.tenant_id = r.tenant_id AND ol.orden_compra_id = r.orden_compra_id
               AND ol.numero_linea = :numeroLineaOrden
             WHERE r.uuid_publico = :recepcionId
            """;

    private final JdbcClient jdbcClient;
    private final TransactionOperations transaction;

    public RecepcionJdbcWriteAdapter(JdbcClient jdbcClient, TransactionOperations transaction) {
        this.jdbcClient = jdbcClient;
        this.transaction = transaction;
    }

    @Override
    public Optional<RecepcionExistente> findPorBusinessUuid(UUID tenantId, UUID businessUuid) {
        return jdbcClient.sql(POR_BUSINESS_UUID)
                .param("tenantId", tenantId)
                .param("businessUuid", businessUuid)
                .query((rs, rowNumber) -> new RecepcionExistente(
                        JdbcColumns.uuid(rs, "uuid_publico"), rs.getString("huella_solicitud")))
                .optional();
    }

    @Override
    public GuardadoOutcome insertar(Recepcion recepcion, UUID businessUuid, String huella) {
        try {
            transaction.executeWithoutResult(status -> persistir(recepcion, businessUuid, huella));
            return GuardadoOutcome.GUARDADO;
        } catch (DataIntegrityViolationException | FilaNoInsertada exception) {
            return GuardadoOutcome.DUPLICADO;
        }
    }

    private void persistir(Recepcion recepcion, UUID businessUuid, String huella) {
        exigirUnaFila(insertarRecepcion(recepcion, businessUuid, huella));
        recepcion.lineas().forEach(linea -> exigirUnaFila(insertarLinea(recepcion, linea)));
    }

    private int insertarRecepcion(Recepcion recepcion, UUID businessUuid, String huella) {
        var datos = recepcion.datos();
        return jdbcClient.sql(INSERTAR_RECEPCION)
                .param("recepcionId", recepcion.id())
                .param("tenantId", recepcion.tenantId())
                .param("ordenId", recepcion.ordenCompraId())
                .param("almacenId", datos.almacenId())
                .param("numero", datos.numero())
                .param("guiaRemitente", datos.guiaRemisionRemitente())
                .param("guiaTransportista", datos.guiaRemisionTransportista())
                .param("documentoTipo", datos.documentoProveedorTipo())
                .param("documentoSerie", datos.documentoProveedorSerie())
                .param("documentoNumero", datos.documentoProveedorNumero())
                .param("fechaRecepcion", JdbcColumns.offset(recepcion.fechaRecepcion()))
                .param("temperatura", datos.temperaturaRecepcionC())
                .param("humedad", datos.humedadRelativaPct())
                .param("recibidoPor", recepcion.recibidoPor())
                .param("observacion", datos.observacion())
                .param("businessUuid", businessUuid)
                .param("huella", huella)
                .update();
    }

    private int insertarLinea(Recepcion recepcion, LineaRecepcion linea) {
        return jdbcClient.sql(INSERTAR_LINEA)
                .param("lineaId", linea.id())
                .param("recepcionId", recepcion.id())
                .param("numeroLinea", linea.numeroLinea())
                .param("numeroLineaOrden", linea.numeroLineaOrden())
                .param("numeroLote", linea.numeroLote())
                .param("fechaFabricacion", linea.fechaFabricacion())
                .param("fechaVencimiento", linea.fechaVencimiento())
                .param("recibida", linea.cantidadRecibida())
                .param("aceptada", linea.cantidadAceptada())
                .param("rechazada", linea.cantidadRechazada())
                .param("costoUnitario", linea.costoUnitario())
                .param("decision", linea.decisionCalidad())
                .param("motivo", linea.motivoDecision())
                .param("observacion", linea.observacion())
                .param("fechaRecepcion", JdbcColumns.offset(recepcion.fechaRecepcion()))
                .param("recibidoPor", recepcion.recibidoPor())
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
