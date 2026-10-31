package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ListarEmpresasQuery;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarEmpresasHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ListarEmpresasHandler handler = new ListarEmpresasHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();

    @Test
    void listsWithValidPagination() {
        when(readPort.findEmpresas(TENANT_ID, "", 0, 20))
                .thenReturn(new PaginaResult<>(List.of(), 0, 20, 0));

        var result = handler.execute(new ListarEmpresasQuery(TENANT_ID, "", 0, 20));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void rejectsNegativePage() {
        var result = handler.execute(new ListarEmpresasQuery(TENANT_ID, "", -1, 20));

        assertThat(result.isFailure()).isTrue();
        result.fold(page -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            return null;
        });
    }

    @Test
    void rejectsSizeBelowOne() {
        var result = handler.execute(new ListarEmpresasQuery(TENANT_ID, "", 0, 0));

        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void rejectsSizeAboveHundred() {
        var result = handler.execute(new ListarEmpresasQuery(TENANT_ID, "", 0, 101));

        assertThat(result.isFailure()).isTrue();
    }
}
