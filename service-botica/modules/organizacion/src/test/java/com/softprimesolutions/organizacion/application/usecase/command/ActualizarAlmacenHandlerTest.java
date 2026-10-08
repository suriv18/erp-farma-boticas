package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarAlmacenHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final ActualizarAlmacenHandler handler = new ActualizarAlmacenHandler(readPort, writePort, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();
    private static final UUID ALMACEN_ID = UUID.randomUUID();

    private AlmacenResult existingAlmacen(boolean activo) {
        return new AlmacenResult(
                ALMACEN_ID, TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", "VENTA",
                true, true, true, true, false, null, null, activo,
                Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    private ActualizarAlmacenCommand validCommand(boolean activo) {
        return new ActualizarAlmacenCommand(
                ALMACEN_ID, TENANT_ID, "Almacén central renovado", "GENERAL",
                true, true, true, true, false, null, null, activo);
    }

    @Test
    void updatesWhenExists() {
        when(readPort.findAlmacenById(TENANT_ID, ALMACEN_ID)).thenReturn(Optional.of(existingAlmacen(true)));
        when(writePort.save(any(Almacen.class))).thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.UPDATED);

        var result = handler.execute(validCommand(true));

        assertThat(result.isSuccess()).isTrue();
        result.fold(almacen -> {
            assertThat(almacen.nombre()).isEqualTo("Almacén central renovado");
            assertThat(almacen.tipo()).isEqualTo("GENERAL");
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void activatesWhenCommandActivoDiffersFromCurrent() {
        when(readPort.findAlmacenById(TENANT_ID, ALMACEN_ID)).thenReturn(Optional.of(existingAlmacen(false)));
        when(writePort.save(any(Almacen.class))).thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.UPDATED);

        var result = handler.execute(validCommand(true));

        assertThat(result.isSuccess()).isTrue();
        result.fold(almacen -> {
            assertThat(almacen.activo()).isTrue();
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void deactivatesWhenCommandActivoDiffersFromCurrent() {
        when(readPort.findAlmacenById(TENANT_ID, ALMACEN_ID)).thenReturn(Optional.of(existingAlmacen(true)));
        when(writePort.save(any(Almacen.class))).thenReturn(OrganizacionWritePort.SaveAlmacenOutcome.UPDATED);

        var result = handler.execute(validCommand(false));

        assertThat(result.isSuccess()).isTrue();
        result.fold(almacen -> {
            assertThat(almacen.activo()).isFalse();
            return null;
        }, error -> { throw new AssertionError(error.message()); });
        verify(writePort, times(1)).save(any(Almacen.class));
    }

    @Test
    void returnsNotFoundWhenAlmacenMissing() {
        when(readPort.findAlmacenById(TENANT_ID, ALMACEN_ID)).thenReturn(Optional.empty());

        var result = handler.execute(validCommand(true));

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        when(readPort.findAlmacenById(TENANT_ID, ALMACEN_ID)).thenReturn(Optional.of(existingAlmacen(true)));
        var invalidCommand = new ActualizarAlmacenCommand(
                ALMACEN_ID, TENANT_ID, "A", "GENERAL", true, true, true, true, false, null, null, true);

        var result = handler.execute(invalidCommand);

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenTipoInvalido() {
        when(readPort.findAlmacenById(TENANT_ID, ALMACEN_ID)).thenReturn(Optional.of(existingAlmacen(true)));
        var invalidCommand = new ActualizarAlmacenCommand(
                ALMACEN_ID, TENANT_ID, "Almacén válido", "NO_EXISTE", true, true, true, true, false,
                null, null, true);

        var result = handler.execute(invalidCommand);

        assertThat(result.isFailure()).isTrue();
        result.fold(almacen -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
