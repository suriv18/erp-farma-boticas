package com.softprimesolutions.clientes.infrastructure.persistence.write.adapter;

import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.clientes.infrastructure.persistence.write.mapper.ClientesWriteMapper;
import com.softprimesolutions.clientes.infrastructure.persistence.write.repository.ClienteJpaRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class ClientesJpaWriteAdapter implements ClientesWritePort {

    private static final String TENANT_ID = "SELECT id FROM sch_admin.tenant WHERE uuid_publico = :tenantUuid";
    private static final String TIPO_DOCUMENTO_ACTIVO = """
            SELECT EXISTS (
                SELECT 1 FROM sch_catalogo.tipo_documento_identidad
                 WHERE codigo = :codigo AND es_activo = '1')
            """;

    private final ClienteJpaRepository repository;
    private final JdbcClient jdbcClient;

    public ClientesJpaWriteAdapter(ClienteJpaRepository repository, JdbcClient jdbcClient) {
        this.repository = repository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveClienteOutcome save(Cliente cliente) {
        var tenantId = jdbcClient.sql(TENANT_ID)
                .param("tenantUuid", cliente.tenantId().value())
                .query(Long.class)
                .optional();
        if (tenantId.isEmpty()) return SaveClienteOutcome.TENANT_NOT_FOUND;
        if (cliente.tipoDocumento() != null) {
            if (!tipoDocumentoActivo(cliente.tipoDocumento())) return SaveClienteOutcome.TIPO_DOCUMENTO_INVALIDO;
            if (repository.existsByTenantIdAndTipoDocumentoAndNumeroDocumento(
                    tenantId.get(), cliente.tipoDocumento(), cliente.numeroDocumento())) {
                return SaveClienteOutcome.DUPLICATE_DOCUMENTO;
            }
        }
        try {
            repository.saveAndFlush(ClientesWriteMapper.toEntity(cliente, tenantId.get()));
            return SaveClienteOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveClienteOutcome.DUPLICATE_DOCUMENTO;
        }
    }

    private boolean tipoDocumentoActivo(String codigo) {
        return jdbcClient.sql(TIPO_DOCUMENTO_ACTIVO)
                .param("codigo", codigo)
                .query(Boolean.class)
                .single();
    }
}
