package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ListarProductosReguladosQuery;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarProductosReguladosHandlerTest {

    @Test
    void returnsAPageOfProductosReguladosFromReadPort() {
        var resumen = new ProductoReguladoResumen(UUID.randomUUID(), "Paracetamol 500mg", null, "VIGENTE");
        var page = new PaginaResult<>(List.of(resumen), 0, 20, 1);
        var readPort = new FakeCatalogoReadPort(page);
        var handler = new ListarProductosReguladosHandler(readPort);

        var result = handler.execute(new ListarProductosReguladosQuery(null, null, null, 0, 20));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getOrElse(error -> null).items().size());
    }

    @Test
    void failsWithValidationErrorWhenPaginationIsInvalid() {
        var readPort = new FakeCatalogoReadPort(new PaginaResult<>(List.of(), 0, 20, 0));
        var handler = new ListarProductosReguladosHandler(readPort);

        var result = handler.execute(new ListarProductosReguladosQuery(null, null, null, -1, 20));

        assertTrue(result.isFailure());
        assertEquals("CAT_PAGINACION_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    private static final class FakeCatalogoReadPort implements CatalogoReadPort {
        private final PaginaResult<ProductoReguladoResumen> page;

        private FakeCatalogoReadPort(PaginaResult<ProductoReguladoResumen> page) {
            this.page = page;
        }

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
        public List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<MarcaResult> findMarcas(UUID tenantId, String estado) { throw new UnsupportedOperationException(); }

        @Override
        public List<CategoriaProductoResult> findCategoriasProducto(UUID tenantId, UUID categoriaPadreId, String estado) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<ProductoReguladoResumen> findProductosRegulados(String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size) { return this.page; }

        @Override
        public PaginaResult<SkuResumen> findSkus(UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado, int page, int size) { throw new UnsupportedOperationException(); }
    }
}
