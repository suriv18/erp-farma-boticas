package com.softprimesolutions.inventario.application.usecase.query;

import static com.softprimesolutions.inventario.InventarioFixtures.ALMACEN;
import static com.softprimesolutions.inventario.InventarioFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.inventario.InventarioFixtures.LOTE;
import static com.softprimesolutions.inventario.InventarioFixtures.SKU;
import static com.softprimesolutions.inventario.InventarioFixtures.TENANT;
import static com.softprimesolutions.inventario.InventarioFixtures.loteResult;
import static com.softprimesolutions.inventario.InventarioFixtures.paginaPosiciones;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.inventario.application.dto.query.ListarPosicionesQuery;
import com.softprimesolutions.inventario.application.dto.query.ObtenerLoteQuery;
import com.softprimesolutions.inventario.application.port.out.InventarioReadPort;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QueryHandlersTest {

    private final InventarioReadPort readPort = mock(InventarioReadPort.class);
    private final ListarPosicionesHandler listar = new ListarPosicionesHandler(readPort);
    private final ObtenerLoteHandler obtener = new ObtenerLoteHandler(readPort);

    private static ListarPosicionesQuery query(int page, int size) {
        return new ListarPosicionesQuery(TENANT, ESTABLECIMIENTO, ALMACEN, SKU, page, size);
    }

    @Test
    void listsThePositionsOfTheTenantWithTheRequestedFilters() {
        when(readPort.findPosiciones(TENANT, ESTABLECIMIENTO, ALMACEN, SKU, 0, 20)).thenReturn(paginaPosiciones());

        var page = listar.execute(query(0, 20)).fold(found -> found, error -> null);

        assertThat(page.items()).hasSize(1);
    }

    @Test
    void acceptsTheLimitsOfThePageSize() {
        when(readPort.findPosiciones(TENANT, ESTABLECIMIENTO, ALMACEN, SKU, 0, 1)).thenReturn(paginaPosiciones());
        when(readPort.findPosiciones(TENANT, ESTABLECIMIENTO, ALMACEN, SKU, 0, 100)).thenReturn(paginaPosiciones());

        assertThat(listar.execute(query(0, 1)).isSuccess()).isTrue();
        assertThat(listar.execute(query(0, 100)).isSuccess()).isTrue();
    }

    @Test
    void rejectsInvalidPagination() {
        for (var invalid : new ListarPosicionesQuery[] {query(-1, 20), query(0, 0), query(0, 101)}) {
            var error = listar.execute(invalid).fold(page -> null, failure -> failure);

            assertThat(error.category()).isEqualTo(ErrorCategory.VALIDATION);
            assertThat(error.code()).isEqualTo("INV_PAGINACION_INVALIDA");
        }
        verify(readPort, never()).findPosiciones(TENANT, ESTABLECIMIENTO, ALMACEN, SKU, -1, 20);
    }

    @Test
    void getsALoteById() {
        when(readPort.findLoteById(TENANT, LOTE)).thenReturn(Optional.of(loteResult()));

        var lote = obtener.execute(new ObtenerLoteQuery(TENANT, LOTE)).fold(found -> found, error -> null);

        assertThat(lote.id()).isEqualTo(LOTE);
    }

    @Test
    void reportsAMissingLoteAsNotFound() {
        when(readPort.findLoteById(TENANT, LOTE)).thenReturn(Optional.empty());

        var error = obtener.execute(new ObtenerLoteQuery(TENANT, LOTE)).fold(lote -> null, failure -> failure);

        assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
    }
}
