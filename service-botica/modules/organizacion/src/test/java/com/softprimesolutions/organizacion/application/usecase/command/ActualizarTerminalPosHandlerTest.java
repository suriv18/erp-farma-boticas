package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarTerminalPosHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final ActualizarTerminalPosHandler handler =
            new ActualizarTerminalPosHandler(readPort, writePort, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();
    private static final UUID TERMINAL_ID = UUID.randomUUID();

    private TerminalPosResult existingTerminal(String estado) {
        return new TerminalPosResult(
                TERMINAL_ID, TENANT_ID, ESTABLECIMIENTO_ID, "POS-01", "Caja 1", "B001", "F001",
                "SN-0001", "host-01", "192.168.0.10", "PRN-01", true, estado,
                Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    private ActualizarTerminalPosCommand validCommand(String estado) {
        return new ActualizarTerminalPosCommand(
                TERMINAL_ID, TENANT_ID, "Caja 1 renovada", "B002", "F002", "SN-0002", "host-02",
                "192.168.0.20", "PRN-02", false, estado);
    }

    @Test
    void updatesWhenExists() {
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.of(existingTerminal("ACTIVO")));
        when(writePort.save(any(TerminalPos.class))).thenReturn(OrganizacionWritePort.SaveTerminalOutcome.UPDATED);

        var result = handler.execute(validCommand("ACTIVO"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(terminal -> {
            assertThat(terminal.nombre()).isEqualTo("Caja 1 renovada");
            assertThat(terminal.hostname()).isEqualTo("host-02");
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void changesEstadoWhenCommandEstadoDiffersFromCurrent() {
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.of(existingTerminal("ACTIVO")));
        when(writePort.save(any(TerminalPos.class))).thenReturn(OrganizacionWritePort.SaveTerminalOutcome.UPDATED);

        var result = handler.execute(validCommand("BLOQUEADO"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(terminal -> {
            assertThat(terminal.estado()).isEqualTo("BLOQUEADO");
            return null;
        }, error -> { throw new AssertionError(error.message()); });
        verify(writePort, times(1)).save(any(TerminalPos.class));
    }

    @Test
    void returnsNotFoundWhenTerminalMissing() {
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.empty());

        var result = handler.execute(validCommand("ACTIVO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.of(existingTerminal("ACTIVO")));
        var invalidCommand = new ActualizarTerminalPosCommand(
                TERMINAL_ID, TENANT_ID, "A", "B002", "F002", "SN-0002", "host-02", "192.168.0.20",
                "PRN-02", false, "ACTIVO");

        var result = handler.execute(invalidCommand);

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenEstadoInvalido() {
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.of(existingTerminal("ACTIVO")));
        var invalidCommand = new ActualizarTerminalPosCommand(
                TERMINAL_ID, TENANT_ID, "Caja válida", "B002", "F002", "SN-0002", "host-02",
                "192.168.0.20", "PRN-02", false, "NO_EXISTE");

        var result = handler.execute(invalidCommand);

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
