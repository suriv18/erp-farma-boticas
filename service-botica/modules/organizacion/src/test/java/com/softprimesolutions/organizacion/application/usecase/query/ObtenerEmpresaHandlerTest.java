package com.softprimesolutions.organizacion.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.organizacion.application.dto.query.ObtenerEmpresaQuery;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerEmpresaHandlerTest {

    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final ObtenerEmpresaHandler handler = new ObtenerEmpresaHandler(readPort);
    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID EMPRESA_ID = UUID.randomUUID();

    @Test
    void returnsEmpresaWhenFound() {
        var empresaResult = new EmpresaOperadoraResult(
                EMPRESA_ID, TENANT_ID, "20123456789", "Boticas SAC", null, null, null, null, null,
                null, "PEN", "America/Lima", false, "ACTIVO", Instant.now(), null);
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.of(empresaResult));

        var result = handler.execute(new ObtenerEmpresaQuery(TENANT_ID, EMPRESA_ID));

        assertThat(result.isSuccess()).isTrue();
    }

    @Test
    void returnsNotFoundWhenMissing() {
        when(readPort.findEmpresaById(TENANT_ID, EMPRESA_ID)).thenReturn(Optional.empty());

        var result = handler.execute(new ObtenerEmpresaQuery(TENANT_ID, EMPRESA_ID));

        assertThat(result.isFailure()).isTrue();
        result.fold(empresa -> null, error -> {
            assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
            return null;
        });
    }
}
