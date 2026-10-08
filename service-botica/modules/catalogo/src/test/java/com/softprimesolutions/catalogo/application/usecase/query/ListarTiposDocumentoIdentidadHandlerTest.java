package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ListarTiposDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarTiposDocumentoIdentidadHandlerTest {

    @Test
    void returnsPageFromReadPort() {
        var tipo = new TipoDocumentoIdentidadResult("1", "DNI", "Documento Nacional de Identidad", 8, 8, "ACTIVO");
        var page = new PaginaResult<>(List.of(tipo), 0, 20, 1);
        var readPort = new StubCatalogoReadPort(page);
        var handler = new ListarTiposDocumentoIdentidadHandler(readPort);

        var result = handler.execute(new ListarTiposDocumentoIdentidadQuery(null, 0, 20));

        assertTrue(result.isSuccess());
        result.fold(
                success -> {
                    assertEquals(1, success.items().size());
                    assertEquals("1", success.items().get(0).codigo());
                    return null;
                },
                error -> {
                    throw new AssertionError("expected success but got " + error);
                });
    }

    @Test
    void rejectsInvalidPageSize() {
        var readPort = new StubCatalogoReadPort(new PaginaResult<>(List.of(), 0, 20, 0));
        var handler = new ListarTiposDocumentoIdentidadHandler(readPort);

        var result = handler.execute(new ListarTiposDocumentoIdentidadQuery(null, 0, 0));

        assertTrue(result.isFailure());
        assertEquals("CAT_PAGINACION_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsNegativePage() {
        var readPort = new StubCatalogoReadPort(new PaginaResult<>(List.of(), 0, 20, 0));
        var handler = new ListarTiposDocumentoIdentidadHandler(readPort);

        var result = handler.execute(new ListarTiposDocumentoIdentidadQuery(null, -1, 20));

        assertTrue(result.isFailure());
        assertEquals("CAT_PAGINACION_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsPageSizeGreaterThanMax() {
        var readPort = new StubCatalogoReadPort(new PaginaResult<>(List.of(), 0, 20, 0));
        var handler = new ListarTiposDocumentoIdentidadHandler(readPort);

        var result = handler.execute(new ListarTiposDocumentoIdentidadQuery(null, 0, 101));

        assertTrue(result.isFailure());
        assertEquals("CAT_PAGINACION_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    private record StubCatalogoReadPort(PaginaResult<TipoDocumentoIdentidadResult> page) implements CatalogoReadPort {

        @Override
        public List<CondicionVentaResult> findCondicionesVenta(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<FormaFarmaceuticaResult> findFormasFarmaceuticas(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<ViaAdministracionResult> findViasAdministracion(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<UnidadMedidaResult> findUnidadesMedida(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<ClasificacionControladaResult> findClasificacionesControladas(String estado) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado, int page, int size) { return this.page(); }

        @Override
        public List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<MarcaResult> findMarcas(UUID tenantId, String texto, String estado, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<CategoriaProductoResult> findCategoriasProducto(UUID tenantId, String texto, UUID categoriaPadreId, String estado, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<ProductoReguladoResumen> findProductosRegulados(String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<SkuResumen> findSkus(UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<RubroComercialResult> findRubrosComerciales(UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size) { throw new UnsupportedOperationException(); }
    }
}
