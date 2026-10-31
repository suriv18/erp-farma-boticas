package com.softprimesolutions.clientes.application.usecase.query;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.clientes.application.dto.query.ListarClientesQuery;
import com.softprimesolutions.clientes.application.dto.query.ObtenerClienteQuery;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.PaginaResult;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ConsultasClientesHandlersTest {

    private static final UUID TENANT_ID = UUID.randomUUID();
    private static final UUID CLIENTE_ID = UUID.randomUUID();

    private final ClientesReadPort readPort = mock(ClientesReadPort.class);
    private final ObtenerClienteHandler obtener = new ObtenerClienteHandler(readPort);
    private final ListarClientesHandler listar = new ListarClientesHandler(readPort);

    private static <T> T valueOf(Result<T, ApplicationError> result) {
        return result.fold(value -> value, error -> null);
    }

    private static ClienteResult cliente() {
        return new ClienteResult(CLIENTE_ID, TENANT_ID, "NATURAL", "1", "12345678", "Ana", "Pérez",
                null, null, null, null, "ACTIVO", null, Instant.parse("2026-10-30T15:00:00Z"), null);
    }

    @Test
    void getsAClientByItsIdentifier() {
        var cliente = cliente();
        when(readPort.findClienteById(TENANT_ID, CLIENTE_ID)).thenReturn(Optional.of(cliente));

        var result = obtener.execute(new ObtenerClienteQuery(TENANT_ID, CLIENTE_ID));

        assertThat(valueOf(result)).isSameAs(cliente);
    }

    @Test
    void reportsAMissingClient() {
        when(readPort.findClienteById(TENANT_ID, CLIENTE_ID)).thenReturn(Optional.empty());

        var error = obtener.execute(new ObtenerClienteQuery(TENANT_ID, CLIENTE_ID))
                .fold(value -> null, detail -> detail);

        assertThat(error.code()).isEqualTo("CLI_CLIENTE_NO_ENCONTRADO");
        assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
    }

    @Test
    void listsClientsWithValidPagination() {
        var pagina = new PaginaResult<>(List.of(cliente()), 0, 20, 1);
        when(readPort.findClientes(TENANT_ID, "ana", 0, 20)).thenReturn(pagina);

        var result = listar.execute(new ListarClientesQuery(TENANT_ID, "ana", 0, 20));

        assertThat(valueOf(result)).isSameAs(pagina);
    }

    @Test
    void rejectsInvalidPagination() {
        for (var query : List.of(
                new ListarClientesQuery(TENANT_ID, "", -1, 20),
                new ListarClientesQuery(TENANT_ID, "", 0, 0),
                new ListarClientesQuery(TENANT_ID, "", 0, 101))) {
            var error = listar.execute(query).fold(value -> null, detail -> detail);

            assertThat(error.code()).isEqualTo("CLI_PAGINACION_INVALIDA");
            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
        }
        verify(readPort, never()).findClientes(TENANT_ID, "", 0, 20);
    }

    @Test
    void requiresItsPortAndItsQuery() {
        assertThatThrownBy(() -> new ObtenerClienteHandler(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> new ListarClientesHandler(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> obtener.execute(null)).isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> listar.execute(null)).isInstanceOf(NullPointerException.class);
    }

    @Test
    void thePageCopiesItsItems() {
        var items = new java.util.ArrayList<>(List.of(cliente()));
        var pagina = new PaginaResult<>(items, 0, 20, 1);
        items.clear();

        assertThat(pagina.items()).hasSize(1);
    }
}
