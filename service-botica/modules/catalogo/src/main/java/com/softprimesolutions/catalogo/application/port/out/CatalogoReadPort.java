package com.softprimesolutions.catalogo.application.port.out;

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
import java.util.List;
import java.util.UUID;

public interface CatalogoReadPort {

    List<CondicionVentaResult> findCondicionesVenta(String estado);

    List<FormaFarmaceuticaResult> findFormasFarmaceuticas(String estado);

    List<ViaAdministracionResult> findViasAdministracion(String estado);

    List<UnidadMedidaResult> findUnidadesMedida(String estado);

    List<ClasificacionControladaResult> findClasificacionesControladas(String estado);

    PaginaResult<TipoDocumentoIdentidadResult> findTiposDocumentoIdentidad(String estado, int page, int size);

    List<PrincipioActivoResult> findPrincipiosActivos(String texto, String estado);

    PaginaResult<MarcaResult> findMarcas(UUID tenantId, String texto, String estado, int page, int size);

    PaginaResult<CategoriaProductoResult> findCategoriasProducto(
            UUID tenantId, String texto, UUID categoriaPadreId, String estado, int page, int size);

    PaginaResult<ProductoReguladoResumen> findProductosRegulados(
            String texto, String condicionVentaCodigo, String estadoRegulatorio, int page, int size);

    PaginaResult<SkuResumen> findSkus(
            UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
            int page, int size);

    PaginaResult<RubroComercialResult> findRubrosComerciales(
            UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size);
}
