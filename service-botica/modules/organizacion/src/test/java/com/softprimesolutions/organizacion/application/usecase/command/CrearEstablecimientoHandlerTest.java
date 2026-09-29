package com.softprimesolutions.organizacion.application.usecase.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.command.CrearEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearEstablecimientoHandlerTest {

    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final IdentifierGenerator identifierGenerator = () -> UUID.fromString(
            "22222222-2222-2222-2222-222222222222");
    private final ClockPort clock = () -> Instant.parse("2026-09-27T00:00:00Z");
    private final CrearEstablecimientoHandler handler =
            new CrearEstablecimientoHandler(writePort, identifierGenerator, clock);

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    private CrearEstablecimientoCommand validCommand() {
        return new CrearEstablecimientoCommand(
                TENANT_ID, EMPRESA_ID, "EST01", "Botica Central", "BOTICA", null, "1234", null,
                null, null, null, null, null, null, null, true, false, false, "STORE_EDGE",
                "America/Lima");
    }

    @Test
    void createsEstablecimientoWhenValid() {
        when(writePort.save(any(Establecimiento.class)))
                .thenReturn(OrganizacionWritePort.SaveEstablecimientoOutcome.CREATED);

        var result = handler.execute(validCommand());

        assertThat(result.isSuccess()).isTrue();
        result.fold(establecimiento -> {
            assertThat(establecimiento.codigo()).isEqualTo("EST01");
            assertThat(establecimiento.tenantId()).isEqualTo(TENANT_ID);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsValidationErrorWhenDomainRejects() {
        var command = new CrearEstablecimientoCommand(
                TENANT_ID, EMPRESA_ID, "EST01", "Botica Central", "BOTICA", null, null, null,
                null, null, null, null, null, null, null, true, false, false, "STORE_EDGE",
                "America/Lima");

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsNotFoundWhenEmpresaMissing() {
        when(writePort.save(any(Establecimiento.class)))
                .thenReturn(OrganizacionWritePort.SaveEstablecimientoOutcome.EMPRESA_NOT_FOUND);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }

    @Test
    void returnsConflictWhenCodigoDuplicated() {
        when(writePort.save(any(Establecimiento.class)))
                .thenReturn(OrganizacionWritePort.SaveEstablecimientoOutcome.DUPLICATE_CODIGO);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            return null;
        });
    }

    @Test
    void returnsConflictWhenDigemidDuplicated() {
        when(writePort.save(any(Establecimiento.class)))
                .thenReturn(OrganizacionWritePort.SaveEstablecimientoOutcome.DUPLICATE_DIGEMID);

        var result = handler.execute(validCommand());

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.CONFLICT);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenTenantIdIsNull() {
        var command = new CrearEstablecimientoCommand(
                null, EMPRESA_ID, "EST01", "Botica Central", "BOTICA", null, "1234", null,
                null, null, null, null, null, null, null, true, false, false, "STORE_EDGE",
                "America/Lima");

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenEmpresaIdIsNull() {
        var command = new CrearEstablecimientoCommand(
                TENANT_ID, null, "EST01", "Botica Central", "BOTICA", null, "1234", null,
                null, null, null, null, null, null, null, true, false, false, "STORE_EDGE",
                "America/Lima");

        var result = handler.execute(command);

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
