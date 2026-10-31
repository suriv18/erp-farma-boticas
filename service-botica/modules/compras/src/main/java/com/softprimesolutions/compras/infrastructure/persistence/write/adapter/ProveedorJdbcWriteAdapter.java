package com.softprimesolutions.compras.infrastructure.persistence.write.adapter;

import com.softprimesolutions.compras.application.port.out.GuardadoOutcome;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.domain.model.DatosProveedor;
import com.softprimesolutions.compras.domain.model.Proveedor;
import com.softprimesolutions.compras.infrastructure.persistence.JdbcColumns;
import com.softprimesolutions.compras.infrastructure.persistence.ProveedorRows;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;

@Repository
public class ProveedorJdbcWriteAdapter implements ProveedorWritePort {

    private static final String POR_ID = ProveedorRows.SELECT + " AND p.uuid_publico = :proveedorId";
    private static final String INSERTAR = """
            INSERT INTO sch_abastecimiento.proveedor
                (uuid_publico, tenant_id, tipo_documento, numero_documento, razon_social, nombre_comercial,
                 direccion, ubigeo, telefono, email, contacto_nombre, contacto_telefono, contacto_email,
                 condicion_pago_default, dias_credito_default, moneda_default, es_laboratorio, es_importador,
                 es_distribuidor, calificacion, estado, created_at, created_by)
            SELECT :proveedorId, t.id, :tipoDocumento, :numeroDocumento, :razonSocial, :nombreComercial,
                   :direccion, :ubigeo, :telefono, CAST(:email AS citext), :contactoNombre, :contactoTelefono,
                   CAST(:contactoEmail AS citext), :condicionPagoDefault, :diasCreditoDefault, :monedaDefault,
                   :esLaboratorio, :esImportador, :esDistribuidor, :calificacion, :estado, :createdAt, :createdBy
              FROM sch_admin.tenant t
             WHERE t.uuid_publico = :tenantId
            ON CONFLICT (tenant_id, tipo_documento, numero_documento) WHERE es_activo = '1' DO NOTHING
            """;
    private static final String ACTUALIZAR = """
            UPDATE sch_abastecimiento.proveedor
               SET tipo_documento = :tipoDocumento, numero_documento = :numeroDocumento,
                   razon_social = :razonSocial, nombre_comercial = :nombreComercial, direccion = :direccion,
                   ubigeo = :ubigeo, telefono = :telefono, email = CAST(:email AS citext),
                   contacto_nombre = :contactoNombre, contacto_telefono = :contactoTelefono,
                   contacto_email = CAST(:contactoEmail AS citext), condicion_pago_default = :condicionPagoDefault,
                   dias_credito_default = :diasCreditoDefault, moneda_default = :monedaDefault,
                   es_laboratorio = :esLaboratorio, es_importador = :esImportador,
                   es_distribuidor = :esDistribuidor, calificacion = :calificacion, estado = :estado,
                   updated_at = :updatedAt, updated_by = :updatedBy
             WHERE uuid_publico = :proveedorId AND es_activo = '1'
               AND tenant_id = (SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantId)
            """;

    private final JdbcClient jdbcClient;

    public ProveedorJdbcWriteAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    public Optional<Proveedor> findById(UUID tenantId, UUID proveedorId) {
        return jdbcClient.sql(POR_ID)
                .param("tenantId", tenantId)
                .param("proveedorId", proveedorId)
                .query((rs, rowNumber) -> ProveedorRows.map(rs, tenantId))
                .optional();
    }

    @Override
    public GuardadoOutcome insertar(Proveedor proveedor) {
        return ejecutar(INSERTAR, proveedor) == 1 ? GuardadoOutcome.GUARDADO : GuardadoOutcome.DUPLICADO;
    }

    @Override
    public GuardadoOutcome actualizar(Proveedor proveedor) {
        try {
            ejecutar(ACTUALIZAR, proveedor);
            return GuardadoOutcome.GUARDADO;
        } catch (DataIntegrityViolationException exception) {
            return GuardadoOutcome.DUPLICADO;
        }
    }

    private int ejecutar(String sql, Proveedor proveedor) {
        var datos = proveedor.datos();
        return jdbcClient.sql(sql)
                .param("proveedorId", proveedor.id())
                .param("tenantId", proveedor.tenantId())
                .param("tipoDocumento", datos.tipoDocumento())
                .param("numeroDocumento", datos.numeroDocumento())
                .param("razonSocial", datos.razonSocial())
                .param("nombreComercial", datos.nombreComercial())
                .param("direccion", datos.direccion())
                .param("ubigeo", datos.ubigeo())
                .param("telefono", datos.telefono())
                .param("email", datos.email())
                .param("contactoNombre", datos.contactoNombre())
                .param("contactoTelefono", datos.contactoTelefono())
                .param("contactoEmail", datos.contactoEmail())
                .param("condicionPagoDefault", datos.condicionPagoDefault())
                .param("diasCreditoDefault", datos.diasCreditoDefault())
                .param("monedaDefault", datos.monedaDefault())
                .param("esLaboratorio", datos.esLaboratorio())
                .param("esImportador", datos.esImportador())
                .param("esDistribuidor", datos.esDistribuidor())
                .param("calificacion", datos.calificacion())
                .param("estado", proveedor.estado().name())
                .param("createdAt", JdbcColumns.offset(proveedor.createdAt()))
                .param("createdBy", proveedor.createdBy())
                .param("updatedAt", JdbcColumns.offset(proveedor.updatedAt()))
                .param("updatedBy", proveedor.updatedBy())
                .update();
    }
}
