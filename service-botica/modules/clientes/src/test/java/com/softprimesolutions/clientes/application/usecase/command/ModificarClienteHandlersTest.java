package com.softprimesolutions.clientes.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.clientes.application.dto.command.ActualizarClienteCommand;
import com.softprimesolutions.clientes.application.dto.command.CambiarEstadoClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort.UpdateClienteOutcome;
import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ModificarClienteHandlersTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.randomUUID();
    private static final Instant CREATED = Instant.parse("2026-10-30T15:00:00Z");
    private static final Instant NOW = Instant.parse("2026-11-02T09:00:00Z");

    private final ClientesReadPort readPort = mock(ClientesReadPort.class);
    private final ClientesWritePort writePort = mock(ClientesWritePort.class);
    private final ClockPort clock = () -> NOW;
    private final ActualizarClienteHandler actualizar = new ActualizarClienteHandler(readPort, writePort, clock);
    private final CambiarEstadoClienteHandler cambiarEstado =
            new CambiarEstadoClienteHandler(readPort, writePort, clock);

    private static ClienteResult existente() {
        return new ClienteResult(CLIENTE_ID, TENANT_ID, "NATURAL", "1", "12345678", "Ana", "Pérez", null, null,
                null, null, "ACTIVO", null, CREATED, null);
    }

    private static ActualizarClienteCommand actualizacion(String nombres) {
        return new ActualizarClienteCommand(TENANT_ID, CLIENTE_ID, nombres, "Gómez", null, "ana@correo.pe", null,
                null);
    }

    private static CambiarEstadoClienteCommand cambio(String estado, String motivo) {
        return new CambiarEstadoClienteCommand(TENANT_ID, CLIENTE_ID, estado, motivo);
    }

    private void clienteExiste() {
        when(readPort.findClienteById(TENANT_ID, CLIENTE_ID)).thenReturn(Optional.of(existente()));
    }

    private static ApplicationError errorOf(Result<ClienteResult, ApplicationError> result) {
        assertThat(result.isFailure()).isTrue();
        return result.fold(value -> null, error -> error);
    }

    @Test
    void updatesTheDetailsAndPersistsTheChange() {
        clienteExiste();
        when(writePort.update(any(Cliente.class))).thenReturn(UpdateClienteOutcome.UPDATED);

        var result = actualizar.execute(actualizacion("Ana Lucía")).fold(value -> value, error -> null);

        assertThat(result.nombres()).isEqualTo("Ana Lucía");
        assertThat(result.apellidos()).isEqualTo("Gómez");
        assertThat(result.updatedAt()).isEqualTo(NOW);
        assertThat(result.createdAt()).isEqualTo(CREATED);
        var captor = ArgumentCaptor.forClass(Cliente.class);
        verify(writePort).update(captor.capture());
        assertThat(captor.getValue().numeroDocumento()).isEqualTo("12345678");
    }

    @Test
    void rejectsAnInvalidUpdateWithoutPersisting() {
        clienteExiste();

        var error = errorOf(actualizar.execute(actualizacion(null)));

        assertThat(error.code()).isEqualTo("CLI_CLIENTE_INVALIDO");
        assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
        verify(writePort, never()).update(any());
    }

    @Test
    void reportsAMissingClientOnUpdate() {
        when(readPort.findClienteById(TENANT_ID, CLIENTE_ID)).thenReturn(Optional.empty());

        var error = errorOf(actualizar.execute(actualizacion("Ana")));

        assertThat(error.code()).isEqualTo("CLI_CLIENTE_NO_ENCONTRADO");
        assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
        verify(writePort, never()).update(any());
    }

    @Test
    void reportsAMissingClientWhenThePersistenceFindsNoRow() {
        clienteExiste();
        when(writePort.update(any(Cliente.class))).thenReturn(UpdateClienteOutcome.NOT_FOUND);

        var error = errorOf(actualizar.execute(actualizacion("Ana")));

        assertThat(error.code()).isEqualTo("CLI_CLIENTE_NO_ENCONTRADO");
    }

    @Test
    void blocksAClientWithItsReason() {
        clienteExiste();
        when(writePort.update(any(Cliente.class))).thenReturn(UpdateClienteOutcome.UPDATED);

        var result = cambiarEstado.execute(cambio("BLOQUEADO", "Fraude reiterado")).fold(value -> value, error -> null);

        assertThat(result.estado()).isEqualTo("BLOQUEADO");
        assertThat(result.motivoBloqueo()).isEqualTo("Fraude reiterado");
        assertThat(result.updatedAt()).isEqualTo(NOW);
    }

    @Test
    void requiresTheReasonToBlock() {
        clienteExiste();

        var error = errorOf(cambiarEstado.execute(cambio("BLOQUEADO", null)));

        assertThat(error.code()).isEqualTo("CLI_CLIENTE_INVALIDO");
        verify(writePort, never()).update(any());
    }

    @Test
    void rejectsAnUnknownStatusWithoutReadingTheClient() {
        var error = errorOf(cambiarEstado.execute(cambio("SUSPENDIDO", null)));

        assertThat(error.code()).isEqualTo("CLI_ESTADO_INVALIDO");
        assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
        verify(readPort, never()).findClienteById(any(), any());
    }

    @Test
    void requiresItsCollaboratorsAndTheCommand() {
        assertThatThrownBy(() -> new ActualizarClienteHandler(null, writePort, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ActualizarClienteHandler(readPort, null, clock))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ActualizarClienteHandler(readPort, writePort, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new CambiarEstadoClienteHandler(readPort, writePort, null))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> actualizar.execute(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> cambiarEstado.execute(null)).isInstanceOf(NullPointerException.class);
    }
}
