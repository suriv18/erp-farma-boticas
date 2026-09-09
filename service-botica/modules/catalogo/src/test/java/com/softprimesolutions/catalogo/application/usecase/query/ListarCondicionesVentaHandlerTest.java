package com.softprimesolutions.catalogo.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.catalogo.application.dto.query.ListarCondicionesVentaQuery;
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

class ListarCondicionesVentaHandlerTest {

    @Test
    void returnsCondicionesVentaFromReadPort() {
        var condicion = new CondicionVentaResult(
                "SIN-RECETA", "Sin receta médica", false, false, null, null, null, null, "ACTIVO");
        var readPort = new FakeCatalogoReadPort(List.of(condicion));
        var handler = new ListarCondicionesVentaHandler(readPort);

        var result = handler.execute(new ListarCondicionesVentaQuery(null));

        assertTrue(result.isSuccess());
        assertEquals(1, result.getOrElse(error -> null).size());
    }

    private static final class FakeCatalogoReadPort implements CatalogoReadPort {
        private final List<CondicionVentaResult> condiciones;

        private FakeCatalogoReadPort(List<CondicionVentaResult> condiciones) {
            this.condiciones = condiciones;
        }

        @Override
        public List<CondicionVentaResult> findCondicionesVenta(String estado) { return condiciones; }

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
        public PaginaResult<ProductoReguladoResumen> findProductosRegulados(String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size) { throw new UnsupportedOperationException(); }

        @Override
        public PaginaResult<SkuResumen> findSkus(UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado, int page, int size) { throw new UnsupportedOperationException(); }
    }
}
