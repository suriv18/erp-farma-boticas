package com.softprimesolutions.compras.infrastructure.persistence;

import com.softprimesolutions.compras.domain.model.DatosProveedor;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import com.softprimesolutions.compras.domain.model.Proveedor;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

public final class ProveedorRows {

    public static final String SELECT = """
            SELECT p.uuid_publico, p.tipo_documento, p.numero_documento, p.razon_social, p.nombre_comercial,
                   p.direccion, p.ubigeo, p.telefono, p.email, p.contacto_nombre, p.contacto_telefono,
                   p.contacto_email, p.condicion_pago_default, p.dias_credito_default, p.moneda_default,
                   p.es_laboratorio, p.es_importador, p.es_distribuidor, p.calificacion, p.estado,
                   p.created_at, p.created_by, p.updated_at, p.updated_by
              FROM sch_abastecimiento.proveedor p
              JOIN sch_admin.tenant t ON t.id = p.tenant_id
             WHERE t.uuid_publico = :tenantId AND p.es_activo = '1'
            """;

    private ProveedorRows() {
    }

    public static Proveedor map(ResultSet rs, UUID tenantId) throws SQLException {
        var datos = new DatosProveedor(
                rs.getString("tipo_documento"), rs.getString("numero_documento"), rs.getString("razon_social"),
                rs.getString("nombre_comercial"), rs.getString("direccion"), rs.getString("ubigeo"),
                rs.getString("telefono"), rs.getString("email"), rs.getString("contacto_nombre"),
                rs.getString("contacto_telefono"), rs.getString("contacto_email"),
                rs.getString("condicion_pago_default"), rs.getInt("dias_credito_default"),
                rs.getString("moneda_default"), rs.getBoolean("es_laboratorio"), rs.getBoolean("es_importador"),
                rs.getBoolean("es_distribuidor"), rs.getString("calificacion"));
        return Proveedor.restore(
                JdbcColumns.uuid(rs, "uuid_publico"), tenantId, datos, EstadoProveedor.valueOf(rs.getString("estado")),
                JdbcColumns.instant(rs, "created_at"), rs.getString("created_by"),
                JdbcColumns.instant(rs, "updated_at"), rs.getString("updated_by"));
    }
}
