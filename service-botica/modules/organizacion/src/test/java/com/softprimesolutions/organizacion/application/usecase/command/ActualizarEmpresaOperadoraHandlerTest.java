package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.ActualizarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarEmpresaOperadoraHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final ActualizarEmpresaOperadoraHandler handler =
            new ActualizarEmpresaOperadoraHandler(readPort, writePort, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    private EmpresaOperadoraResult existingEmpresa() {
        return new EmpresaOperadoraResult(
                EMPRESA_ID, TENANT_ID, "20123456786", "Boticas SAC", null, null, null, null, null,
                null, "PEN", "America/Lima", false, "ACTIVO", Instant.parse("2026-01-01T00:00:00Z"), null);
    }

    private ActualizarEmpresaOperadoraCommand validCommand() {
        return new ActualizarEmpresaOperadoraCommand(
                EMPRESA_ID, TENANT_ID, "Boticas del Peru SAC", "Boticas", null, null, null, null, null,
                "PEN", "America/Lima", true);
    }

    @Test
    void updatesWhenExists() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(existingEmpresa()));
        when(writePort.save(any(EmpresaOperadora.class))).thenReturn(OrganizacionWritePort.SaveEmpresaOutcome.UPDATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
        result.fold(empresa -> {
            assertThat(empresa.razonSocial()).isEqualTo("Boticas del Peru SAC");
            assertThat(empresa.ruc()).isEqualTo("20123456786");
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsNotFoundWhenEmpresaMissing() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(existingEmpresa()));
        var invalidCommand = new ActualizarEmpresaOperadoraCommand(
                EMPRESA_ID, TENANT_ID, "A", null, null, null, null, null, null,
                "PEN", "America/Lima", false);

        var result = handler.execute(invalidCommand);

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
