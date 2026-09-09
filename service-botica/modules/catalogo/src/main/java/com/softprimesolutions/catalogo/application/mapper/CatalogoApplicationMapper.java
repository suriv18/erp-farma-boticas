package com.softprimesolutions.catalogo.application.mapper;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.domain.model.CategoriaProducto;
import com.softprimesolutions.catalogo.domain.model.Marca;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.catalogo.domain.model.soporte.CondicionVenta;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;

public final class CatalogoApplicationMapper {

    private CatalogoApplicationMapper() {
    }

    public static CondicionVentaResult toResult(CondicionVenta condicionVenta) {
        return new CondicionVentaResult(
                condicionVenta.codigo(), condicionVenta.denominacion(), condicionVenta.requiereReceta(),
                condicionVenta.requiereRetencion(), condicionVenta.fuente(), condicionVenta.versionFuente(),
                condicionVenta.vigenteDesde(), condicionVenta.vigenteHasta(), condicionVenta.estado().name());
    }

    public static FormaFarmaceuticaResult toResult(FormaFarmaceutica formaFarmaceutica) {
        return new FormaFarmaceuticaResult(
                formaFarmaceutica.codigo(), formaFarmaceutica.denominacion(), formaFarmaceutica.fuente(),
                formaFarmaceutica.estado().name());
    }

    public static ViaAdministracionResult toResult(ViaAdministracion viaAdministracion) {
        return new ViaAdministracionResult(
                viaAdministracion.codigo(), viaAdministracion.denominacion(), viaAdministracion.fuente(),
                viaAdministracion.estado().name());
    }

    public static UnidadMedidaResult toResult(UnidadMedida unidadMedida) {
        return new UnidadMedidaResult(
                unidadMedida.codigo(), unidadMedida.denominacion(), unidadMedida.simbolo(),
                unidadMedida.permiteDecimal(), unidadMedida.fuente(), unidadMedida.estado().name());
    }

    public static ClasificacionControladaResult toResult(ClasificacionControlada clasificacionControlada) {
        return new ClasificacionControladaResult(
                clasificacionControlada.codigo(), clasificacionControlada.denominacion(),
                clasificacionControlada.normaFuente(), clasificacionControlada.requiereRecetaEspecial(),
                clasificacionControlada.retieneReceta(), clasificacionControlada.vigenciaRecetaDias(),
                clasificacionControlada.estado().name());
    }

    public static PrincipioActivoResult toResult(PrincipioActivo principioActivo) {
        return new PrincipioActivoResult(
                principioActivo.id().value(), principioActivo.codigoFuente(), principioActivo.denominacion(),
                principioActivo.nombreNormalizado(), principioActivo.fuente(), principioActivo.estado().name());
    }

    public static MarcaResult toResult(Marca marca) {
        return new MarcaResult(
                marca.id().value(), marca.tenantId().value(), marca.codigo(), marca.nombre(),
                marca.descripcion(), marca.estado().name());
    }

    public static CategoriaProductoResult toResult(CategoriaProducto categoria) {
        return new CategoriaProductoResult(
                categoria.id().value(), categoria.tenantId().value(),
                categoria.categoriaPadreId() == null ? null : categoria.categoriaPadreId().value(),
                categoria.codigo(), categoria.nombre(), categoria.descripcion(), categoria.nivel(),
                categoria.orden(), categoria.estado().name());
    }
}
