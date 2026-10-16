package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarEstablecimientoHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final ActualizarEstablecimientoHandler handler =
            new ActualizarEstablecimientoHandler(readPort, writePort, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();

    private EstablecimientoResult existingEstablecimiento() {
        return new EstablecimientoResult(
                ESTABLECIMIENTO_ID, TENANT_ID, EMPRESA_ID, "EST01", "Botica Central", "BOTICA", null,
                "1234", null, null, null, null, null, null, null, null, true, false, false,
                "STORE_EDGE", "America/Lima", "ACTIVO", Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    private ActualizarEstablecimientoCommand validCommand() {
        return new ActualizarEstablecimientoCommand(
                ESTABLECIMIENTO_ID, TENANT_ID, "Botica Central Renovada", "BOTICA", null, "1234", null,
                null, null, null, null, null, null, null, true, false, false, "STORE_EDGE",
                "America/Lima");
    }

    @Test
    void updatesWhenExists() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID))
                .thenReturn(Optional.of(existingEstablecimiento()));
        when(writePort.save(any(Establecimiento.class)))
                .thenReturn(OrganizacionWritePort.SaveEstablecimientoOutcome.UPDATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
        result.fold(establecimiento -> {
            assertThat(establecimiento.nombre()).isEqualTo("Botica Central Renovada");
            assertThat(establecimiento.codigo()).isEqualTo("EST01");
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsNotFoundWhenEstablecimientoMissing() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID)).thenReturn(Optional.empty());

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID))
                .thenReturn(Optional.of(existingEstablecimiento()));
        var invalidCommand = new ActualizarEstablecimientoCommand(
                ESTABLECIMIENTO_ID, TENANT_ID, "A", "BOTICA", null, "1234", null, null, null, null,
                null, null, null, null, true, false, false, "STORE_EDGE", "America/Lima");

        var result = handler.execute(invalidCommand);

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
