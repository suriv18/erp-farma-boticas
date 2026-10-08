package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEstablecimientoQuery;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerEstablecimientoHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ObtenerEstablecimientoHandler handler = new ObtenerEstablecimientoHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();

    @Test
    void returnsEstablecimientoWhenFound() {
        var establecimientoResult = new EstablecimientoResult(
                ESTABLECIMIENTO_ID, TENANT_ID, EMPRESA_ID, "EST01", "Botica Central", "BOTICA", null,
                "1234", null, null, null, null, null, null, null, null, true, false, false,
                "STORE_EDGE", "America/Lima", "ACTIVO", Instant.now(), null);
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID))
                .thenReturn(Optional.of(establecimientoResult));

        var result = handler.execute(new ObtenerEstablecimientoQuery(TENANT_ID, ESTABLECIMIENTO_ID));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void returnsNotFoundWhenMissing() {
        when(readPort.findEstablecimientoById(TENANT_ID, ESTABLECIMIENTO_ID)).thenReturn(Optional.empty());

        var result = handler.execute(new ObtenerEstablecimientoQuery(TENANT_ID, ESTABLECIMIENTO_ID));

        assertThat(result.isFailure()).isTrue();
        result.fold(establecimiento -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }
}
