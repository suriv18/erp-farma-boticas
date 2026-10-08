package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EstadoEstablecimiento;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CambiarEstadoEstablecimientoHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final CambiarEstadoEstablecimientoHandler handler =
            new CambiarEstadoEstablecimientoHandler(readPort, writePort, () -> NOW);

    private EstablecimientoResult existingEstablecimiento() {
        return new EstablecimientoResult(
                ESTABLECIMIENTO_ID, TENANT_ID, UUID.randomUUID(), "EST001", "Botica Central", "BOTICA", null,
                "0001", "DIG001", "Av. 2", "150101", null, BigDecimal.ONE, BigDecimal.TEN, null, null, true,
                false, false, "ONLINE", "America/Lima", "ACTIVO", Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    @Test
    void changesTheOperationalStatusAndPersistsTheAggregate() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID))
                .thenReturn(Optional.of(existingEstablecimiento()));
        var saved = ArgumentCaptor.forClass(Establecimiento.class);

        var result = handler.execute(
                new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO_ID, TENANT_ID, "REMODELACION"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(establecimiento -> {
            assertThat(establecimiento.estadoOperativo()).isEqualTo("REMODELACION");
            assertThat(establecimiento.updatedAt()).isEqualTo(NOW);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
        verify(writePort).save(saved.capture());
        assertThat(saved.getValue().estadoOperativo()).isEqualTo(EstadoEstablecimiento.REMODELACION);
    }

    @Test
    void returnsNotFoundWhenTheEstablecimientoDoesNotExist() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID)).thenReturn(Optional.empty());

        var result = handler.execute(
                new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO_ID, TENANT_ID, "SUSPENDIDO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
        verify(writePort, never()).save(any(Establecimiento.class));
    }

    @Test
    void rejectsAnUnknownStatusWithoutReadingTheEstablecimiento() {
        var result = handler.execute(
                new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO_ID, TENANT_ID, "BLOQUEADO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            assertThat(error.code()).isEqualTo("ORG_ESTABLECIMIENTO_ESTADO_INVALIDO");
            assertThat(error.message()).contains("ACTIVO, SUSPENDIDO, CLAUSURADO, REMODELACION");
            return null;
        });
        verify(readPort, never()).findEstablecimientoById(any(), any());
        verify(writePort, never()).save(any(Establecimiento.class));
    }

    @Test
    void returnsAValidationErrorWhenTheDomainRejectsTheChange() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID))
                .thenReturn(Optional.of(existingEstablecimiento()));
        ClockPort clockWithoutInstant = () -> null;
        var handlerWithoutInstant =
                new CambiarEstadoEstablecimientoHandler(readPort, writePort, clockWithoutInstant);

        var result = handlerWithoutInstant.execute(
                new CambiarEstadoEstablecimientoCommand(ESTABLECIMIENTO_ID, TENANT_ID, "CLAUSURADO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
        verify(writePort, never()).save(any(Establecimiento.class));
    }
}
