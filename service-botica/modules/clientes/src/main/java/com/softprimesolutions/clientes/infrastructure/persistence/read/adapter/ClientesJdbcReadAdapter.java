package com.softprimesolutions.clientes.infrastructure.persistence.read.adapter;

import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.PaginaResult;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.infrastructure.persistence.read.mapper.ClienteRowMapper;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ClientesJdbcReadAdapter implements ClientesReadPort {

    private static final String SELECT = """
            SELECT c.uuid_publico, t.uuid_publico AS tenant_uuid, c.tipo_cliente, c.tipo_documento,
                   c.numero_documento, c.nombres, c.apellidos, c.razon_social, c.email, c.telefono,
                   c.fecha_nacimiento, c.estado, c.created_at
            """;
    private static final String FROM = """
            FROM sch_venta.cliente c
            JOIN sch_admin.tenant t ON t.id = c.tenant_id
            """;
    private static final String SEARCH = """
            WHERE t.uuid_publico = :tenantId AND c.es_activo = '1'
              AND (:search = '' OR LOWER(COALESCE(c.numero_documento, '')) LIKE :pattern
                OR LOWER(COALESCE(c.nombres, '')) LIKE :pattern OR LOWER(COALESCE(c.apellidos, '')) LIKE :pattern
                OR LOWER(COALESCE(c.razon_social, '')) LIKE :pattern)
            """;
    private static final String ORDER_AND_PAGE =
            "ORDER BY COALESCE(c.apellidos, c.razon_social, ''), COALESCE(c.nombres, ''), c.id "
                    + "LIMIT :limit OFFSET :offset";
    private static final String BY_ID = "WHERE t.uuid_publico = :tenantId AND c.uuid_publico = :clienteId";
    private static final String BY_DOCUMENTO = """
            WHERE t.uuid_publico = :tenantId AND c.es_activo = '1'
              AND c.tipo_documento = :tipoDocumento AND c.numero_documento = :numeroDocumento
            """;

    private final JdbcClient jdbcClient;
    private final ClienteRowMapper rowMapper = new ClienteRowMapper();

    public ClientesJdbcReadAdapter(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional(readOnly = true)
    public PaginaResult<ClienteResult> findClientes(UUID tenantId, String search, int page, int size) {
        var normalized = search == null ? "" : search.trim().toLowerCase(Locale.ROOT);
        var pattern = "%" + normalized + "%";
        var items = jdbcClient.sql(SELECT + FROM + SEARCH + ORDER_AND_PAGE)
                .param("tenantId", tenantId)
                .param("search", normalized)
                .param("pattern", pattern)
                .param("limit", size)
                .param("offset", page * size)
                .query(rowMapper)
                .list();
        var total = jdbcClient.sql("SELECT COUNT(*) " + FROM + SEARCH)
                .param("tenantId", tenantId)
                .param("search", normalized)
                .param("pattern", pattern)
                .query(Long.class)
                .single();
        return new PaginaResult<>(items, page, size, total);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteResult> findClienteById(UUID tenantId, UUID clienteId) {
        return jdbcClient.sql(SELECT + FROM + BY_ID)
                .param("tenantId", tenantId)
                .param("clienteId", clienteId)
                .query(rowMapper)
                .optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ClienteResult> findClienteByDocumento(
            UUID tenantId, String tipoDocumento, String numeroDocumento) {
        return jdbcClient.sql(SELECT + FROM + BY_DOCUMENTO)
                .param("tenantId", tenantId)
                .param("tipoDocumento", tipoDocumento)
                .param("numeroDocumento", numeroDocumento)
                .query(rowMapper)
                .optional();
    }
}
