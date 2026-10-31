package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerTerminalQuery;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerTerminalHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ObtenerTerminalHandler handler = new ObtenerTerminalHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID TERMINAL_ID = UUID.randomUUID();

    @Test
    void returnsTerminalWhenFound() {
        var terminal = new TerminalPosResult(
                TERMINAL_ID, TENANT_ID, UUID.randomUUID(), "POS-01", "Caja 1", "B001", "F001",
                "SN-0001", "host-01", "192.168.0.10", "PRN-01", true, "ACTIVO", Instant.now(), null);
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.of(terminal));

        var result = handler.execute(new ObtenerTerminalQuery(TENANT_ID, TERMINAL_ID));

        assertThat(result.isSuccess()).isTrue();
        result.fold(found -> {
            assertThat(found).isEqualTo(terminal);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsNotFoundWhenMissing() {
        when(readPort.findTerminalById(TENANT_ID, TERMINAL_ID)).thenReturn(Optional.empty());

        var result = handler.execute(new ObtenerTerminalQuery(TENANT_ID, TERMINAL_ID));

        assertThat(result.isFailure()).isTrue();
        result.fold(found -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }
}
