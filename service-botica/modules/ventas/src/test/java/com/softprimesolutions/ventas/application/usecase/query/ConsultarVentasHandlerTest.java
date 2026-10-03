package com.softprimesolutions.ventas.application.usecase.query;

import static com.softprimesolutions.ventas.VentasFixtures.AHORA;
import static com.softprimesolutions.ventas.VentasFixtures.ESTABLECIMIENTO;
import static com.softprimesolutions.ventas.VentasFixtures.TENANT;
import static com.softprimesolutions.ventas.VentasFixtures.TERMINAL;
import static com.softprimesolutions.ventas.VentasFixtures.VENTA;
import static com.softprimesolutions.ventas.VentasFixtures.dec;
import static com.softprimesolutions.ventas.VentasFixtures.ventaResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.dto.query.ListarVentasQuery;
import com.softprimesolutions.ventas.application.dto.query.ObtenerVentaQuery;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ConsultarVentasHandlerTest {

    private final VentasReadPort readPort = mock(VentasReadPort.class);
    private final ConsultarVentasHandler handler = new ConsultarVentasHandler(readPort);

    private static ListarVentasQuery query(int page, int size) {
        return new ListarVentasQuery(TENANT, ESTABLECIMIENTO, AHORA, AHORA.plusSeconds(60), page, size);
    }

    private static ApplicationError error(Result<?, ApplicationError> result) {
        return result.fold(value -> { throw new AssertionError(value); }, error -> error);
    }

    @Test
    void returnsASaleById() {
        when(readPort.findVenta(TENANT, VENTA)).thenReturn(Optional.of(ventaResult()));

        Result<VentaResult, ApplicationError> result = handler.obtener(new ObtenerVentaQuery(TENANT, VENTA));

        assertThat(result.<VentaResult>fold(value -> value, error -> null)).isEqualTo(ventaResult());
    }

    @Test
    void aMissingSaleIsNotFound() {
        var error = error(handler.obtener(new ObtenerVentaQuery(TENANT, VENTA)));

        assertThat(error.code()).isEqualTo("VEN_VENTA_NO_ENCONTRADA");
        assertThat(error.category()).isEqualTo(ErrorCategory.NOT_FOUND);
    }

    @Test
    void listsASalesPage() {
        var pagina = new PaginaResult<>(
                List.of(new VentaResumenResult(VENTA, "EST001-POS01-000001", TERMINAL, AHORA, dec("12.50"), "CONFIRMADA")),
                0, 20, 1L);
        when(readPort.listarVentas(query(0, 20))).thenReturn(pagina);

        var result = handler.listar(query(0, 20));

        assertThat(result.<PaginaResult<VentaResumenResult>>fold(value -> value, error -> null)).isEqualTo(pagina);
    }

    @Test
    void acceptsThePageSizeBoundaries() {
        when(readPort.listarVentas(query(0, 1))).thenReturn(new PaginaResult<>(List.of(), 0, 1, 0L));
        when(readPort.listarVentas(query(3, 100))).thenReturn(new PaginaResult<>(List.of(), 3, 100, 0L));

        assertThat(handler.listar(query(0, 1)).isSuccess()).isTrue();
        assertThat(handler.listar(query(3, 100)).isSuccess()).isTrue();
    }

    @Test
    void rejectsAnOutOfRangePagination() {
        assertThat(error(handler.listar(query(-1, 20))).code()).isEqualTo("VEN_PAGINACION_INVALIDA");
        assertThat(error(handler.listar(query(0, 0))).code()).isEqualTo("VEN_PAGINACION_INVALIDA");
        assertThat(error(handler.listar(query(0, 101))).code()).isEqualTo("VEN_PAGINACION_INVALIDA");
    }

    @Test
    void requiresItsCollaboratorAndTheQueries() {
        assertThatNullPointerException().isThrownBy(() -> new ConsultarVentasHandler(null));
        assertThatNullPointerException().isThrownBy(() -> handler.obtener(null));
        assertThatNullPointerException().isThrownBy(() -> handler.listar(null));
    }
}
