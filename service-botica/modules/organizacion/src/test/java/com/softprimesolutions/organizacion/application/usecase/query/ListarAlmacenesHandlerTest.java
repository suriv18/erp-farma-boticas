package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ListarAlmacenesQuery;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarAlmacenesHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ListarAlmacenesHandler handler = new ListarAlmacenesHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID ESTABLECIMIENTO_ID = UUID.randomUUID();

    @Test
    void returnsPageFromReadPort() {
        var almacen = new AlmacenResult(
                UUID.randomUUID(), TENANT_ID, ESTABLECIMIENTO_ID, "WH-01", "Almacén central", "VENTA",
                true, true, true, true, false, null, null, true, Instant.now(), null);
        var page = new PaginaResult<>(List.of(almacen), 0, 10, 1);
        when(readPort.findAlmacenes(TENANT_ID, ESTABLECIMIENTO_ID, null, 0, 10)).thenReturn(page);

        var result = handler.execute(new ListarAlmacenesQuery(TENANT_ID, ESTABLECIMIENTO_ID, null, 0, 10));

        assertThat(result.isSuccess()).isTrue();
        result.fold(pagina -> {
            assertThat(pagina.items()).containsExactly(almacen);
            return null;
        }, error -> { throw new AssertionError(error.message()); });
    }

    @Test
    void returnsValidationErrorWhenPageIsNegative() {
        var result = handler.execute(new ListarAlmacenesQuery(TENANT_ID, ESTABLECIMIENTO_ID, null, -1, 10));

        assertThat(result.isFailure()).isTrue();
        result.fold(pagina -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenSizeIsBelowOne() {
        var result = handler.execute(new ListarAlmacenesQuery(TENANT_ID, ESTABLECIMIENTO_ID, null, 0, 0));

        assertThat(result.isFailure()).isTrue();
        result.fold(pagina -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void returnsValidationErrorWhenSizeIsAboveHundred() {
        var result = handler.execute(new ListarAlmacenesQuery(TENANT_ID, ESTABLECIMIENTO_ID, null, 0, 101));

        assertThat(result.isFailure()).isTrue();
        result.fold(pagina -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }
}
