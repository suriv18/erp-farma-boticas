package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CrearEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearEmpresaOperadoraHandlerTest {

    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final IdentifierGenerator identifierGenerator = () -> UUID.fromString(
            "11111111-1111-1111-1111-111111111111");
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final CrearEmpresaOperadoraHandler handler =
            new CrearEmpresaOperadoraHandler(writePort, identifierGenerator, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();

    private CrearEmpresaOperadoraCommand validCommand() {
        return new CrearEmpresaOperadoraCommand(
                TENANT_ID, "20123456789", "Boticas SAC", "Boticas", null, null, null, null, null,
                "PEN", "America/Lima", false);
    }

    @Test
    void createsEmpresaWhenValid() {
        when(writePort.save(org.mockito.ArgumentMatchers.any(EmpresaOperadora.class)))
                .thenReturn(OrganizacionWritePort.SaveEmpresaOutcome.CREATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
        result.fold(empresa -> {
            assertThat(empresa.ruc()).isEqualTo("20123456789");
            assertThat(empresa.tenantId()).isEqualTo(TENANT_ID);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        var command = new CrearEmpresaOperadoraCommand(
                TENANT_ID, "30123456789", "Boticas SAC", null, null, null, null, null, null,
                "PEN", "America/Lima", false);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsNotFoundWhenTenantMissing() {
        when(writePort.save(org.mockito.ArgumentMatchers.any(EmpresaOperadora.class)))
                .thenReturn(OrganizacionWritePort.SaveEmpresaOutcome.TENANT_NOT_FOUND);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsConflictWhenRucDuplicated() {
        when(writePort.save(org.mockito.ArgumentMatchers.any(EmpresaOperadora.class)))
                .thenReturn(OrganizacionWritePort.SaveEmpresaOutcome.DUPLICATE_RUC);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenTenantIdIsNull() {
        var command = new CrearEmpresaOperadoraCommand(
                null, "20123456789", "Boticas SAC", "Boticas", null, null, null, null, null,
                "PEN", "America/Lima", false);

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
