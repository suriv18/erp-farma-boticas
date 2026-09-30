package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.EstadoEmpresaOperadora;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CambiarEstadoEmpresaHandlerTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final Instant NOW = Instant.parse("2026-09-30T00:00:00Z");

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final CambiarEstadoEmpresaHandler handler =
            new CambiarEstadoEmpresaHandler(readPort, writePort, () -> NOW);

    private EmpresaOperadoraResult existingEmpresa() {
        return new EmpresaOperadoraResult(
                EMPRESA_ID, TENANT_ID, "20123456789", "Boticas SAC", null, null, null, null, null, null,
                "PEN", "America/Lima", false, "ACTIVO", Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    @Test
    void changesTheStatusAndPersistsTheAggregate() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(existingEmpresa()));
        var saved = ArgumentCaptor.forClass(EmpresaOperadora.class);

        var result = handler.execute(new CambiarEstadoEmpresaCommand(EMPRESA_ID, TENANT_ID, "SUSPENDIDO"));

        assertThat(result.isSuccess()).isTrue();
        result.fold(empresa -> {
            assertThat(empresa.estado()).isEqualTo("SUSPENDIDO");
            assertThat(empresa.updatedAt()).isEqualTo(NOW);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
        verify(writePort).save(saved.capture());
        assertThat(saved.getValue().estado()).isEqualTo(EstadoEmpresaOperadora.SUSPENDIDO);
    }

    @Test
    void returnsNotFoundWhenTheEmpresaDoesNotExist() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        var result = handler.execute(new CambiarEstadoEmpresaCommand(EMPRESA_ID, TENANT_ID, "SUSPENDIDO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
        verify(writePort, never()).save(any(EmpresaOperadora.class));
    }

    @Test
    void rejectsAnUnknownStatusWithoutReadingTheEmpresa() {
        var result = handler.execute(new CambiarEstadoEmpresaCommand(EMPRESA_ID, TENANT_ID, "FOO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            assertThat(error.code()).isEqualTo("ORG_EMPRESA_ESTADO_INVALIDO");
            assertThat(error.message()).contains("ACTIVO, SUSPENDIDO, BLOQUEADO");
            return null;
        });
        verify(readPort, never()).findEmpresaById(any(), any());
        verify(writePort, never()).save(any(EmpresaOperadora.class));
    }

    @Test
    void returnsAValidationErrorWhenTheDomainRejectsTheChange() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(existingEmpresa()));
        ClockPort clockWithoutInstant = () -> null;
        var handlerWithoutInstant = new CambiarEstadoEmpresaHandler(readPort, writePort, clockWithoutInstant);

        var result = handlerWithoutInstant.execute(
                new CambiarEstadoEmpresaCommand(EMPRESA_ID, TENANT_ID, "BLOQUEADO"));

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
        verify(writePort, never()).save(any(EmpresaOperadora.class));
    }
}
