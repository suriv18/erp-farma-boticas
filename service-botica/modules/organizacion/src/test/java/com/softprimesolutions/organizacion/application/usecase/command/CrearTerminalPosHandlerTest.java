package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CrearTerminalPosCommand;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.TerminalPos;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearTerminalPosHandlerTest {

    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final IdentifierGenerator identifierGenerator = () -> UUID.fromString(
            "33333333-3333-3333-3333-333333333333");
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final CrearTerminalPosHandler handler =
            new CrearTerminalPosHandler(writePort, identifierGenerator, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();

    private CrearTerminalPosCommand validCommand() {
        return new CrearTerminalPosCommand(
                TENANT_ID, ESTABLECIMIENTO_ID, "POS-01", "Caja 1", "B001", "F001",
                "SN-0001", "host-01", "192.168.0.10", "PRN-01", true);
    }

    @Test
    void createsTerminalWhenValid() {
        when(writePort.save(any(TerminalPos.class))).thenReturn(OrganizacionWritePort.SaveTerminalOutcome.CREATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        var command = new CrearTerminalPosCommand(
                TENANT_ID, ESTABLECIMIENTO_ID, " ", "Caja 1", "B001", "F001",
                "SN-0001", "host-01", "192.168.0.10", "PRN-01", true);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsNotFoundWhenEstablecimientoMissing() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.ESTABLECIMIENTO_NOT_FOUND);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsConflictWhenCodigoDuplicated() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_CODIGO);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenTenantIdIsNull() {
        var command = new CrearTerminalPosCommand(
                null, ESTABLECIMIENTO_ID, "POS-01", "Caja 1", "B001", "F001",
                "SN-0001", "host-01", "192.168.0.10", "PRN-01", true);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenEstablecimientoIdIsNull() {
        var command = new CrearTerminalPosCommand(
                TENANT_ID, null, "POS-01", "Caja 1", "B001", "F001",
                "SN-0001", "host-01", "192.168.0.10", "PRN-01", true);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsConflictWhenEstablecimientoNoOperativo() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.ESTABLECIMIENTO_NO_OPERATIVO);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            assertThat(error.code()).isEqualTo("ORG_ESTABLECIMIENTO_NO_OPERATIVO");
            assertThat(error.message()).isEqualTo(
                    "El establecimiento no está operativo (suspendido o clausurado); no admite terminales POS nuevos.");
            return null;
        });
    }

    @Test
    void returnsConflictWhenSerieBoletaIsAlreadyAssignedInTheEmpresa() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_SERIE_BOLETA);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            assertThat(error.code()).isEqualTo("ORG_TERMINAL_SERIE_BOLETA_DUPLICADA");
            assertThat(error.message()).isEqualTo("La serie B001 ya está asignada a otra caja de esta empresa.");
            return null;
        });
    }

    @Test
    void returnsConflictWhenSerieFacturaIsAlreadyAssignedInTheEmpresa() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.DUPLICATE_SERIE_FACTURA);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.code()).isEqualTo("ORG_TERMINAL_SERIE_FACTURA_DUPLICADA");
            assertThat(error.message()).isEqualTo("La serie F001 ya está asignada a otra caja de esta empresa.");
            return null;
        });
    }

    @Test
    void returnsConflictWhenTheDatabaseRejectsTheTerminal() {
        when(writePort.save(any(TerminalPos.class)))
                .thenReturn(OrganizacionWritePort.SaveTerminalOutcome.INTEGRITY_VIOLATION);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(terminal -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            assertThat(error.code()).isEqualTo("ORG_TERMINAL_NO_GUARDADO");
            return null;
        });
    }
}
