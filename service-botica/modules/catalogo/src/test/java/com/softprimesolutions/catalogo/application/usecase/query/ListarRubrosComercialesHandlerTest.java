package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ListarRubrosComercialesQuery;
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

class ListarRubrosComercialesHandlerTest {

    @Test
    void returnsPageFromReadPort() {
        var tenantId = UUID.randomUUID();
        var rubroId = UUID.randomUUID();
        var page = new PaginaResult<>(
                List.of(new RubroComercialResult(rubroId, tenantId, "FARMA", "Farmacéutico", null, true, 1, "ACTIVO")),
                0, 20, 1);
        var readPort = new StubCatalogoReadPort(page);
        var handler = new ListarRubrosComercialesHandler(readPort);

        var result = handler.execute(new ListarRubrosComercialesQuery(tenantId, null, null, null, 0, 20));

        assertTrue(result.isSuccess());
        result.fold(
                success -> {
                    assertEquals(1, success.items().size());
                    assertEquals("FARMA", success.items().get(0).codigo());
                    return null;
                },
                error -> {
                    throw new AssertionError("expected success but got " + error);
                });
    }

    @Test
    void rejectsInvalidPageSize() {
        var readPort = new StubCatalogoReadPort(new PaginaResult<>(List.of(), 0, 20, 0));
        var handler = new ListarRubrosComercialesHandler(readPort);

        var result = handler.execute(new ListarRubrosComercialesQuery(UUID.randomUUID(), null, null, null, 0, 0));

        assertTrue(result.isFailure());
    }

    private record StubCatalogoReadPort(PaginaResult<RubroComercialResult> page) implements CatalogoReadPort {

        @Override
        public List<CondicionVentaResult> findCondicionesVenta(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<FormaFarmaceuticaResult> findFormasFarmaceuticas(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ViaAdministracionResult> findViasAdministracion(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<UnidadMedidaResult> findUnidadesMedida(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<ClasificacionControladaResult> findClasificacionesControladas(String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<MarcaResult> findMarcas(UUID tenantId, String texto, String estado, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<CategoriaProductoResult> findCategoriasProducto(
                UUID tenantId, String texto, UUID categoriaPadreId, String estado, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<ProductoReguladoResumen> findProductosRegulados(
                String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<SkuResumen> findSkus(
                UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
                int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<RubroComercialResult> findRubrosComerciales(
                UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size) {
            return this.page();
        }
    }
}
