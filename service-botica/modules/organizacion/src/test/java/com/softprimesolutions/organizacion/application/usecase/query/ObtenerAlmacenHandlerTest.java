package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerAlmacenQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerAlmacenHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ObtenerAlmacenHandler handler = new ObtenerAlmacenHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ALMACEN_ID = UUID.randomUUID();

    @Test
    void returnsAlmacenWhenFound() {
        var almacen = new AlmacenResult(
                ALMACEN_ID, TENANT_ID, UUID.randomUUID(), "WH-01", "Almacén central", "VENTA",
                true, true, true, true, false, null, null, true, Instant.now(), null);
        when(readPort.findAlmacenById(TENANT_ID, ALMACEN_ID)).thenReturn(Optional.of(almacen));

        var result = handler.execute(new ObtenerAlmacenQuery(TENANT_ID, ALMACEN_ID));

        assertThat(result.isSuccess()).isTrue();
        result.fold(found -> {
            assertThat(found).isEqualTo(almacen);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsNotFoundWhenMissing() {
        when(readPort.findAlmacenById(TENANT_ID, ALMACEN_ID)).thenReturn(Optional.empty());

        var result = handler.execute(new ObtenerAlmacenQuery(TENANT_ID, ALMACEN_ID));

        assertThat(result.isFailure()).isTrue();
        result.fold(found -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }
}
