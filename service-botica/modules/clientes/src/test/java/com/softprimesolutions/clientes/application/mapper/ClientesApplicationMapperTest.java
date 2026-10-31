package com.softprimesolutions.clientes.application.mapper;

import static org.assertj.core.api.Assertions.assertThat;

import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.clientes.domain.model.EstadoCliente;
import com.softprimesolutions.clientes.domain.model.TipoCliente;
import com.softprimesolutions.clientes.domain.valueobject.ClienteId;
import com.softprimesolutions.clientes.domain.valueobject.TenantId;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientesApplicationMapperTest {

    @Test
    void mapsTheAggregateToItsResult() {
        var id = UUID.randomUUID();
        var tenantId = UUID.randomUUID();
        var now = Instant.parse("2026-10-30T15:00:00Z");
        var cliente = Cliente.restore(
                new ClienteId(id), new TenantId(tenantId), TipoCliente.NATURAL, "1", "12345678", "Ana",
                "Pérez", null, "ana@correo.pe", "999", LocalDate.parse("1990-05-10"),
                EstadoCliente.BLOQUEADO, "Fraude", now, now.plusSeconds(60));

        var result = ClientesApplicationMapper.toResult(cliente);

        assertThat(result.id()).isEqualTo(id);
        assertThat(result.tenantId()).isEqualTo(tenantId);
        assertThat(result.tipoCliente()).isEqualTo("NATURAL");
        assertThat(result.tipoDocumento()).isEqualTo("1");
        assertThat(result.numeroDocumento()).isEqualTo("12345678");
        assertThat(result.nombres()).isEqualTo("Ana");
        assertThat(result.apellidos()).isEqualTo("Pérez");
        assertThat(result.razonSocial()).isNull();
        assertThat(result.email()).isEqualTo("ana@correo.pe");
        assertThat(result.telefono()).isEqualTo("999");
        assertThat(result.fechaNacimiento()).isEqualTo(LocalDate.parse("1990-05-10"));
        assertThat(result.estado()).isEqualTo("BLOQUEADO");
        assertThat(result.motivoBloqueo()).isEqualTo("Fraude");
        assertThat(result.createdAt()).isEqualTo(now);
        assertThat(result.updatedAt()).isEqualTo(now.plusSeconds(60));
    }

    @Test
    void restoresTheAggregateFromItsResult() {
        var now = Instant.parse("2026-10-30T15:00:00Z");
        var result = new com.softprimesolutions.clientes.application.dto.result.ClienteResult(
                UUID.randomUUID(), UUID.randomUUID(), "JURIDICO", "6", "20123456789", null, null,
                "Botica SAC", null, null, null, "BLOQUEADO", "Fraude", now, now.plusSeconds(60));

        var cliente = ClientesApplicationMapper.toDomain(result);

        assertThat(cliente.id().value()).isEqualTo(result.id());
        assertThat(cliente.tenantId().value()).isEqualTo(result.tenantId());
        assertThat(cliente.tipoCliente()).isEqualTo(TipoCliente.JURIDICO);
        assertThat(cliente.estado()).isEqualTo(EstadoCliente.BLOQUEADO);
        assertThat(cliente.motivoBloqueo()).isEqualTo("Fraude");
        assertThat(cliente.razonSocial()).isEqualTo("Botica SAC");
        assertThat(ClientesApplicationMapper.toResult(cliente)).isEqualTo(result);
    }
}
