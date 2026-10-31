package com.softprimesolutions.clientes.api.mapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.softprimesolutions.clientes.api.dto.request.ActualizarClienteRequest;
import com.softprimesolutions.clientes.api.dto.request.CambiarEstadoClienteRequest;
import com.softprimesolutions.clientes.api.dto.request.RegistrarClienteRequest;
import com.softprimesolutions.clientes.api.dto.response.PaginaResponse;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.PaginaResult;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ClientesApiMapperTest {

    private static final UUID TENANT = UUID.randomUUID();
    private static final ClienteResult RESULT = new ClienteResult(UUID.randomUUID(), TENANT, "NATURAL", "1",
            "12345678", "Ana", "Pérez", null, "ana@correo.pe", "999", LocalDate.parse("1990-05-10"), "ACTIVO", null,
            Instant.parse("2026-10-30T15:00:00Z"), null);

    @Test
    void mapsTheRequestToACommandWithTheTenantOfTheSession() {
        var request = new RegistrarClienteRequest("NATURAL", "1", "12345678", "Ana", "Pérez", null,
                "ana@correo.pe", "999", LocalDate.parse("1990-05-10"));

        var command = ClientesApiMapper.toCommand(TENANT, request);

        assertThat(command.tenantId()).isEqualTo(TENANT);
        assertThat(command.tipoCliente()).isEqualTo("NATURAL");
        assertThat(command.tipoDocumento()).isEqualTo("1");
        assertThat(command.numeroDocumento()).isEqualTo("12345678");
        assertThat(command.nombres()).isEqualTo("Ana");
        assertThat(command.apellidos()).isEqualTo("Pérez");
        assertThat(command.razonSocial()).isNull();
        assertThat(command.email()).isEqualTo("ana@correo.pe");
        assertThat(command.telefono()).isEqualTo("999");
        assertThat(command.fechaNacimiento()).isEqualTo(LocalDate.parse("1990-05-10"));
    }

    @Test
    void mapsTheResultWithoutExposingTheTenant() {
        var response = ClientesApiMapper.toResponse(RESULT);

        assertThat(response.id()).isEqualTo(RESULT.id());
        assertThat(response.tipoCliente()).isEqualTo("NATURAL");
        assertThat(response.tipoDocumento()).isEqualTo("1");
        assertThat(response.numeroDocumento()).isEqualTo("12345678");
        assertThat(response.nombres()).isEqualTo("Ana");
        assertThat(response.apellidos()).isEqualTo("Pérez");
        assertThat(response.razonSocial()).isNull();
        assertThat(response.email()).isEqualTo("ana@correo.pe");
        assertThat(response.telefono()).isEqualTo("999");
        assertThat(response.fechaNacimiento()).isEqualTo(LocalDate.parse("1990-05-10"));
        assertThat(response.estado()).isEqualTo("ACTIVO");
        assertThat(response.createdAt()).isEqualTo(RESULT.createdAt());
    }

    @Test
    void mapsAPageKeepingItsMetadata() {
        var page = ClientesApiMapper.toPage(new PaginaResult<>(List.of(RESULT), 2, 20, 41));

        assertThat(page.items()).containsExactly(ClientesApiMapper.toResponse(RESULT));
        assertThat(page.page()).isEqualTo(2);
        assertThat(page.size()).isEqualTo(20);
        assertThat(page.totalElements()).isEqualTo(41);
    }

    @Test
    void thePageResponseCopiesItsItems() {
        var items = new ArrayList<>(List.of(ClientesApiMapper.toResponse(RESULT)));
        var page = new PaginaResponse<>(items, 0, 20, 1);
        items.clear();

        assertThat(page.items()).hasSize(1);
        assertThatThrownBy(() -> page.items().add(null)).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void mapsTheUpdateAndStatusRequestsToCommandsScopedToTheClient() {
        var clienteId = UUID.randomUUID();

        var actualizar = ClientesApiMapper.toCommand(TENANT, clienteId, new ActualizarClienteRequest(
                "Ana", "Pérez", null, "ana@correo.pe", "999", LocalDate.parse("1990-05-10")));
        var estado = ClientesApiMapper.toCommand(TENANT, clienteId,
                new CambiarEstadoClienteRequest("BLOQUEADO", "Fraude"));

        assertThat(actualizar.tenantId()).isEqualTo(TENANT);
        assertThat(actualizar.clienteId()).isEqualTo(clienteId);
        assertThat(actualizar.nombres()).isEqualTo("Ana");
        assertThat(actualizar.apellidos()).isEqualTo("Pérez");
        assertThat(actualizar.razonSocial()).isNull();
        assertThat(actualizar.email()).isEqualTo("ana@correo.pe");
        assertThat(actualizar.telefono()).isEqualTo("999");
        assertThat(actualizar.fechaNacimiento()).isEqualTo(LocalDate.parse("1990-05-10"));
        assertThat(estado.tenantId()).isEqualTo(TENANT);
        assertThat(estado.clienteId()).isEqualTo(clienteId);
        assertThat(estado.estado()).isEqualTo("BLOQUEADO");
        assertThat(estado.motivo()).isEqualTo("Fraude");
    }
}
