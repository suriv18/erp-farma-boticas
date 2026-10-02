package com.softprimesolutions.compras.application.usecase.query;

import static com.softprimesolutions.compras.ComprasFixtures.ORDEN;
import static com.softprimesolutions.compras.ComprasFixtures.PROVEEDOR;
import static com.softprimesolutions.compras.ComprasFixtures.RECEPCION;
import static com.softprimesolutions.compras.ComprasFixtures.TENANT;
import static com.softprimesolutions.compras.ComprasFixtures.error;
import static com.softprimesolutions.compras.ComprasFixtures.ordenNueva;
import static com.softprimesolutions.compras.ComprasFixtures.proveedor;
import static com.softprimesolutions.compras.ComprasFixtures.value;
import static com.softprimesolutions.compras.ComprasResultFixtures.ordenResumen;
import static com.softprimesolutions.compras.ComprasResultFixtures.recepcionResult;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.softprimesolutions.compras.application.dto.query.ListarOrdenesCompraQuery;
import com.softprimesolutions.compras.application.dto.query.ListarProveedoresQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerOrdenCompraQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerProveedorQuery;
import com.softprimesolutions.compras.application.dto.query.ObtenerRecepcionQuery;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.application.mapper.ComprasApplicationMapper;
import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.compras.domain.model.EstadoProveedor;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class QueryHandlersTest {

    private final ComprasReadPort readPort = mock(ComprasReadPort.class);
    private final ConsultarProveedoresHandler proveedores = new ConsultarProveedoresHandler(readPort);
    private final ConsultarOrdenesCompraHandler ordenes = new ConsultarOrdenesCompraHandler(readPort);
    private final ConsultarRecepcionesHandler recepciones = new ConsultarRecepcionesHandler(readPort);

    @Test
    void getsAProveedorOrReportsItDoesNotExist() {
        var result = ComprasApplicationMapper.toResult(proveedor(EstadoProveedor.ACTIVO));
        when(readPort.findProveedor(TENANT, PROVEEDOR)).thenReturn(Optional.of(result));

        assertThat(value(proveedores.obtener(new ObtenerProveedorQuery(TENANT, PROVEEDOR)))).isEqualTo(result);
        assertThat(error(proveedores.obtener(new ObtenerProveedorQuery(TENANT, ORDEN))).code())
                .isEqualTo("COM_PROVEEDOR_NO_ENCONTRADO");
    }

    @Test
    void listsProveedoresTrimmingTheSearchTextAndPassingTheFilters() {
        var pagina = new PaginaResult<ProveedorResult>(List.of(), 1, 50, 0);
        when(readPort.findProveedores(TENANT, "ACTIVO", "lab", 1, 50)).thenReturn(pagina);
        when(readPort.findProveedores(TENANT, null, null, 0, 20)).thenReturn(pagina);

        assertThat(value(proveedores.listar(new ListarProveedoresQuery(TENANT, "ACTIVO", "  lab ", 1, 50))))
                .isSameAs(pagina);
        assertThat(value(proveedores.listar(new ListarProveedoresQuery(TENANT, null, "   ", 0, 20)))).isSameAs(pagina);
        assertThat(value(proveedores.listar(new ListarProveedoresQuery(TENANT, null, null, 0, 20)))).isSameAs(pagina);
    }

    @Test
    void rejectsInvalidPaginationAndAnUnknownStateFilterWhenListingProveedores() {
        for (var pagina : new int[][] {{-1, 20}, {0, 0}, {0, 101}}) {
            assertThat(error(proveedores.listar(
                    new ListarProveedoresQuery(TENANT, null, null, pagina[0], pagina[1]))).code())
                    .isEqualTo("COM_PAGINACION_INVALIDA");
        }
        assertThat(error(proveedores.listar(new ListarProveedoresQuery(TENANT, "XX", null, 0, 20))).code())
                .isEqualTo("COM_FILTRO_ESTADO_INVALIDO");
        verify(readPort, never()).findProveedores(TENANT, "XX", null, 0, 20);
    }

    @Test
    void getsAnOrderOrReportsItDoesNotExist() {
        var result = ComprasApplicationMapper.toResult(ordenNueva());
        when(readPort.findOrden(TENANT, ORDEN)).thenReturn(Optional.of(result));

        assertThat(value(ordenes.obtener(new ObtenerOrdenCompraQuery(TENANT, ORDEN)))).isEqualTo(result);
        assertThat(error(ordenes.obtener(new ObtenerOrdenCompraQuery(TENANT, PROVEEDOR))).code())
                .isEqualTo("COM_ORDEN_NO_ENCONTRADA");
    }

    @Test
    void listsOrdersWithTheirFilters() {
        var pagina = new PaginaResult<>(List.of(ordenResumen()), 0, 20, 1);
        when(readPort.findOrdenes(TENANT, PROVEEDOR, "EMITIDA", 0, 20)).thenReturn(pagina);
        when(readPort.findOrdenes(TENANT, null, null, 0, 20)).thenReturn(pagina);

        assertThat(value(ordenes.listar(new ListarOrdenesCompraQuery(TENANT, PROVEEDOR, "EMITIDA", 0, 20))))
                .isSameAs(pagina);
        assertThat(value(ordenes.listar(new ListarOrdenesCompraQuery(TENANT, null, null, 0, 20)))).isSameAs(pagina);
    }

    @Test
    void rejectsInvalidPaginationAndAnUnknownStateFilterWhenListingOrders() {
        assertThat(error(ordenes.listar(new ListarOrdenesCompraQuery(TENANT, null, null, 0, 101))).code())
                .isEqualTo("COM_PAGINACION_INVALIDA");
        assertThat(error(ordenes.listar(new ListarOrdenesCompraQuery(TENANT, null, "XX", 0, 20))).code())
                .isEqualTo("COM_FILTRO_ESTADO_INVALIDO");
    }

    @Test
    void getsAReceptionOrReportsItDoesNotExist() {
        when(readPort.findRecepcion(TENANT, RECEPCION)).thenReturn(Optional.of(recepcionResult()));

        assertThat(value(recepciones.obtener(new ObtenerRecepcionQuery(TENANT, RECEPCION))))
                .isEqualTo(recepcionResult());
        assertThat(error(recepciones.obtener(new ObtenerRecepcionQuery(TENANT, ORDEN))).code())
                .isEqualTo("COM_RECEPCION_NO_ENCONTRADA");
    }
}
