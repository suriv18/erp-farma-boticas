package com.softprimesolutions.clientes.infrastructure.persistence.write.adapter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.clientes.application.port.out.ClientesWritePort.SaveClienteOutcome;
import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.clientes.domain.model.TipoCliente;
import com.softprimesolutions.clientes.domain.valueobject.ClienteId;
import com.softprimesolutions.clientes.domain.valueobject.TenantId;
import com.softprimesolutions.clientes.infrastructure.JdbcClientStub;
import com.softprimesolutions.clientes.infrastructure.persistence.write.entity.ClienteJpaEntity;
import com.softprimesolutions.clientes.infrastructure.persistence.write.repository.ClienteJpaRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DataIntegrityViolationException;

class ClientesJpaWriteAdapterTest {

    private static final UUID TENANT_UUID = UUID.randomUUID();
    private static final Long TENANT_ID = 7L;

    private final ClienteJpaRepository repository = mock(ClienteJpaRepository.class);
    private final JdbcClientStub jdbc = new JdbcClientStub();
    private final ClientesJpaWriteAdapter adapter = new ClientesJpaWriteAdapter(repository, jdbc.client());

    private void tenantExists(boolean exists, boolean tipoDocumentoActivo) {
        jdbc.answering(sql -> sql.contains("sch_admin.tenant")
                ? (exists ? Optional.of(TENANT_ID) : Optional.empty())
                : tipoDocumentoActivo);
    }

    private static Cliente identificado() {
        return Cliente.create(new ClienteId(UUID.randomUUID()), new TenantId(TENANT_UUID), TipoCliente.NATURAL,
                "1", "12345678", "Ana", "Pérez", null, null, null, null,
                Instant.parse("2026-10-30T15:00:00Z")).fold(cliente -> cliente, error -> null);
    }

    private static Cliente anonimo() {
        return Cliente.create(new ClienteId(UUID.randomUUID()), new TenantId(TENANT_UUID),
                TipoCliente.SIN_IDENTIFICAR, null, null, null, null, null, null, null, null,
                Instant.parse("2026-10-30T15:00:00Z")).fold(cliente -> cliente, error -> null);
    }

    @Test
    void savesAnIdentifiedClientWithItsTenantPrimaryKey() {
        tenantExists(true, true);
        var cliente = identificado();

        assertThat(adapter.save(cliente)).isEqualTo(SaveClienteOutcome.CREATED);

        var captor = ArgumentCaptor.forClass(ClienteJpaEntity.class);
        verify(repository).saveAndFlush(captor.capture());
        assertThat(captor.getValue().getTenantId()).isEqualTo(TENANT_ID);
        assertThat(captor.getValue().getUuidPublico()).isEqualTo(cliente.id().value());
        assertThat(jdbc.boundParams()).containsEntry("tenantUuid", TENANT_UUID).containsEntry("codigo", "1");
    }

    @Test
    void savesAnAnonymousClientWithoutCheckingTheDocument() {
        tenantExists(true, false);

        assertThat(adapter.save(anonimo())).isEqualTo(SaveClienteOutcome.CREATED);

        verify(repository, never()).existsByTenantIdAndTipoDocumentoAndNumeroDocumento(anyLong(), anyString(),
                anyString());
        assertThat(jdbc.boundParams()).doesNotContainKey("codigo");
    }

    @Test
    void reportsAMissingTenantWithoutPersisting() {
        tenantExists(false, true);

        assertThat(adapter.save(identificado())).isEqualTo(SaveClienteOutcome.TENANT_NOT_FOUND);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void rejectsADocumentTypeThatIsNotActiveInTheCatalog() {
        tenantExists(true, false);

        assertThat(adapter.save(identificado())).isEqualTo(SaveClienteOutcome.TIPO_DOCUMENTO_INVALIDO);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void reportsADuplicateDocumentFoundBeforeSaving() {
        tenantExists(true, true);
        when(repository.existsByTenantIdAndTipoDocumentoAndNumeroDocumento(TENANT_ID, "1", "12345678"))
                .thenReturn(true);

        assertThat(adapter.save(identificado())).isEqualTo(SaveClienteOutcome.DUPLICATE_DOCUMENTO);

        verify(repository, never()).saveAndFlush(any());
    }

    @Test
    void reportsADuplicateDocumentRaisedByTheDatabase() {
        tenantExists(true, true);
        when(repository.saveAndFlush(any())).thenThrow(new DataIntegrityViolationException("uk_cliente_documento"));

        assertThat(adapter.save(identificado())).isEqualTo(SaveClienteOutcome.DUPLICATE_DOCUMENTO);
    }
}
