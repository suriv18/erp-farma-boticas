package com.softprimesolutions.clientes.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.clientes.application.dto.command.RegistrarClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.RegistroClienteResult;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort.SaveClienteOutcome;
import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RegistrarClienteHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final Instant NOW = Instant.parse("2026-10-30T15:00:00Z");

    private final ClientesWritePort writePort = mock(ClientesWritePort.class);
    private final ClientesReadPort readPort = mock(ClientesReadPort.class);
    private final IdentifierGenerator identifierGenerator = () -> CLIENTE_ID;
    private final ClockPort clock = () -> NOW;
    private final RegistrarClienteHandler handler =
            new RegistrarClienteHandler(writePort, readPort, identifierGenerator, clock);

    private static RegistrarClienteCommand command(UUID tenantId, String tipoCliente) {
        return new RegistrarClienteCommand(
                tenantId, tipoCliente, "1", "12345678", "Ana", "Pérez", null, null, null, null);
    }

    private static ApplicationError errorOf(Result<RegistroClienteResult, ApplicationError> result) {
        assertThat(result.isFailure()).isTrue();
        return result.fold(value -> null, error -> error);
    }

    private static ClienteResult existente() {
        return new ClienteResult(UUID.randomUUID(), TENANT_ID, "NATURAL", "1", "12345678", "Ana", "Pérez",
                null, null, null, null, "ACTIVO", NOW);
    }

    @Test
    void registersTheClientWhenItIsValid() {
        when(writePort.save(any(Cliente.class))).thenReturn(SaveClienteOutcome.CREATED);

        var result = handler.execute(command(TENANT_ID, "NATURAL"));

        var registro = result.fold(value -> value, error -> null);
        assertThat(registro.creado()).isTrue();
        assertThat(registro.cliente().id()).isEqualTo(CLIENTE_ID);
        assertThat(registro.cliente().tenantId()).isEqualTo(TENANT_ID);
        assertThat(registro.cliente().estado()).isEqualTo("ACTIVO");
    }

    @Test
    void returnsTheExistingClientWhenTheDocumentIsAlreadyRegistered() {
        var existente = existente();
        when(writePort.save(any(Cliente.class))).thenReturn(SaveClienteOutcome.DUPLICATE_DOCUMENTO);
        when(readPort.findClienteByDocumento(TENANT_ID, "1", "12345678")).thenReturn(Optional.of(existente));

        var registro = handler.execute(command(TENANT_ID, "NATURAL")).fold(value -> value, error -> null);

        assertThat(registro.creado()).isFalse();
        assertThat(registro.cliente()).isSameAs(existente);
    }

    @Test
    void reportsAConflictWhenTheDuplicateCannotBeFound() {
        when(writePort.save(any(Cliente.class))).thenReturn(SaveClienteOutcome.DUPLICATE_DOCUMENTO);
        when(readPort.findClienteByDocumento(TENANT_ID, "1", "12345678")).thenReturn(Optional.empty());

        var error = errorOf(handler.execute(command(TENANT_ID, "NATURAL")));

        assertThat(error.code()).isEqualTo("CLI_CLIENTE_DOCUMENTO_DUPLICADO");
        assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
    }

    @Test
    void reportsAMissingTenant() {
        when(writePort.save(any(Cliente.class))).thenReturn(SaveClienteOutcome.TENANT_NOT_FOUND);

        var error = errorOf(handler.execute(command(TENANT_ID, "NATURAL")));

        assertThat(error.code()).isEqualTo("CLI_TENANT_NO_ENCONTRADO");
        assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
    }

    @Test
    void reportsADocumentTypeOutsideTheCatalog() {
        when(writePort.save(any(Cliente.class))).thenReturn(SaveClienteOutcome.TIPO_DOCUMENTO_INVALIDO);

        var error = errorOf(handler.execute(command(TENANT_ID, "NATURAL")));

        assertThat(error.code()).isEqualTo("CLI_TIPO_DOCUMENTO_INVALIDO");
        assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
    }

    @Test
    void rejectsAnUnknownClientType() {
        var error = errorOf(handler.execute(command(TENANT_ID, "VIP")));

        assertThat(error.code()).isEqualTo("CLI_TIPO_CLIENTE_INVALIDO");
        assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
        verify(writePort, never()).save(any());
    }

    @Test
    void returnsTheDomainValidationErrorWithoutPersisting() {
        var error = errorOf(handler.execute(command(null, "NATURAL")));

        assertThat(error.code()).isEqualTo("CLI_CLIENTE_INVALIDO");
        assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
        verify(writePort, never()).save(any());
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatThrownBy(() -> new RegistrarClienteHandler(null, readPort, identifierGenerator, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RegistrarClienteHandler(writePort, null, identifierGenerator, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RegistrarClienteHandler(writePort, readPort, null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new RegistrarClienteHandler(writePort, readPort, identifierGenerator, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> handler.execute(null)).isInstanceOf(NullPointerException.class);
    }
}
