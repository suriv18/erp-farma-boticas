package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ListarTerminalesQuery;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarTerminalesHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ListarTerminalesHandler handler = new ListarTerminalesHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();

    @Test
    void returnsPageFromReadPort() {
        var terminal = new TerminalPosResult(
                UUID.randomUUID(), TENANT_ID, ESTABLECIMIENTO_ID, "POS-01", "Caja 1", "B001", "F001",
                "SN-0001", "host-01", "192.168.0.10", "PRN-01", true, "ACTIVO", Instant.now(), null);
        var page = new PaginaResult<>(List.of(terminal), 0, 10, 1);
        when(readPort.findTerminales(TENANT_ID, ESTABLECIMIENTO_ID, null, 0, 10)).thenReturn(page);

        var result = handler.execute(new ListarTerminalesQuery(TENANT_ID, ESTABLECIMIENTO_ID, null, 0, 10));

        assertThat(result.isSuccess()).isTrue();
        result.fold(pagina -> {
            assertThat(pagina.items()).containsExactly(terminal);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsValidationErrorWhenPageIsNegative() {
        var result = handler.execute(new ListarTerminalesQuery(TENANT_ID, ESTABLECIMIENTO_ID, null, -1, 10));

        assertThat(result.isFailure()).isTrue();
        result.fold(pagina -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenSizeIsBelowOne() {
        var result = handler.execute(new ListarTerminalesQuery(TENANT_ID, ESTABLECIMIENTO_ID, null, 0, 0));

        assertThat(result.isFailure()).isTrue();
        result.fold(pagina -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenSizeIsAboveHundred() {
        var result = handler.execute(new ListarTerminalesQuery(TENANT_ID, ESTABLECIMIENTO_ID, null, 0, 101));

        assertThat(result.isFailure()).isTrue();
        result.fold(pagina -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
