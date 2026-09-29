package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CrearAlmacenCommand;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearAlmacenHandlerTest {

    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final IdentifierGenerator identifierGenerator = () -> UUID.fromString(
            "22222222-2222-2222-2222-222222222222");
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final CrearAlmacenHandler handler = new CrearAlmacenHandler(writePort, identifierGenerator, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();

    private CrearAlmacenCommand validCommand() {
        return new CrearAlmacenCommand(
                TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", "VENTA",
                true, true, true, true, false, null, null);
    }

    @Test
    void createsAlmacenWhenValid() {
        when(writePort.save(any(com.softprimesolutions.organizacion.domain.model.Almacen.class)))
                .thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.CREATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        var command = new CrearAlmacenCommand(
                TENANT_ID, ESTABLECIMIENTO_ID, " ", "Almacén central", "VENTA",
                true, true, true, true, false, null, null);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenTipoInvalido() {
        var command = new CrearAlmacenCommand(
                TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", "NO_EXISTE",
                true, true, true, true, false, null, null);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsNotFoundWhenEstablecimientoMissing() {
        when(writePort.save(any(com.softprimesolutions.organizacion.domain.model.Almacen.class)))
                .thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.ESTABLECIMIENTO_NOT_FOUND);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsConflictWhenCodigoDuplicated() {
        when(writePort.save(any(com.softprimesolutions.organizacion.domain.model.Almacen.class)))
                .thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.DUPLICATE_CODIGO);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenTenantIdIsNull() {
        var command = new CrearAlmacenCommand(
                null, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", "VENTA",
                true, true, true, true, false, null, null);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenEstablecimientoIdIsNull() {
        var command = new CrearAlmacenCommand(
                TENANT_ID, null, "WH-01", "Almacén central", "VENTA",
                true, true, true, true, false, null, null);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
