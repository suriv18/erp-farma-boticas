package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstructuraCorporativaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerEstructuraCorporativaHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ObtenerEstructuraCorporativaHandler handler =
            new ObtenerEstructuraCorporativaHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    void returnsStructureFromReadPort() {
        var expected = new EstructuraCorporativaResult(Instant.now(), List.of());
        when(readPort.findEstructuraCorporativa(TENANT_ID)).thenReturn(expected);

        var result = handler.execute(new ObtenerEstructuraCorporativaQuery(TENANT_ID));

        assertThat(result.isSuccess()).isTrue();
        result.fold(structure -> {
            assertThat(structure).isEqualTo(expected);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }
}
