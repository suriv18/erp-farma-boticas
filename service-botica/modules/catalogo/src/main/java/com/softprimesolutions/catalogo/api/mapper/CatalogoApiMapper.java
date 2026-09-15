package com.softprimesolutions.catalogo.api.mapper;

import com.softprimesolutions.catalogo.api.dto.request.AgregarCodigoBarraRequest;
import com.softprimesolutions.catalogo.api.dto.request.AsociarPrincipioActivoRequest;
import com.softprimesolutions.catalogo.api.dto.request.CategoriaProductoRequest;
import com.softprimesolutions.catalogo.api.dto.request.ClasificacionControladaRequest;
import com.softprimesolutions.catalogo.api.dto.request.CondicionVentaRequest;
import com.softprimesolutions.catalogo.api.dto.request.FormaFarmaceuticaRequest;
import com.softprimesolutions.catalogo.api.dto.request.MarcaRequest;
import com.softprimesolutions.catalogo.api.dto.request.PrincipioActivoRequest;
import com.softprimesolutions.catalogo.api.dto.request.ProductoReguladoRequest;
import com.softprimesolutions.catalogo.api.dto.request.SkuRequest;
import com.softprimesolutions.catalogo.api.dto.request.UnidadMedidaRequest;
import com.softprimesolutions.catalogo.api.dto.request.ViaAdministracionRequest;
import com.softprimesolutions.catalogo.api.dto.response.CategoriaProductoResponse;
import com.softprimesolutions.catalogo.api.dto.response.ClasificacionControladaResponse;
import com.softprimesolutions.catalogo.api.dto.response.CodigoBarraSkuResponse;
import com.softprimesolutions.catalogo.api.dto.response.CondicionVentaResponse;
import com.softprimesolutions.catalogo.api.dto.response.FormaFarmaceuticaResponse;
import com.softprimesolutions.catalogo.api.dto.response.MarcaResponse;
import com.softprimesolutions.catalogo.api.dto.response.PaginaResponse;
import com.softprimesolutions.catalogo.api.dto.response.PrincipioActivoAsociadoResponse;
import com.softprimesolutions.catalogo.api.dto.response.PrincipioActivoResponse;
import com.softprimesolutions.catalogo.api.dto.response.ProductoReguladoResponse;
import com.softprimesolutions.catalogo.api.dto.response.ProductoReguladoResumenResponse;
import com.softprimesolutions.catalogo.api.dto.response.SkuResponse;
import com.softprimesolutions.catalogo.api.dto.response.SkuResumenResponse;
import com.softprimesolutions.catalogo.api.dto.response.UnidadMedidaResponse;
import com.softprimesolutions.catalogo.api.dto.response.ViaAdministracionResponse;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarCategoriaProductoCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarClasificacionControladaCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarFormaFarmaceuticaCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarMarcaCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarProductoReguladoCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarSkuCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarUnidadMedidaCommand;
import com.softprimesolutions.catalogo.application.dto.command.ActualizarViaAdministracionCommand;
import com.softprimesolutions.catalogo.application.dto.command.AgregarCodigoBarraCommand;
import com.softprimesolutions.catalogo.application.dto.command.AsociarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearCategoriaProductoCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearClasificacionControladaCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearCondicionVentaCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearFormaFarmaceuticaCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearMarcaCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearProductoReguladoCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearSkuCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearUnidadMedidaCommand;
import com.softprimesolutions.catalogo.application.dto.command.CrearViaAdministracionCommand;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResumen;
import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import java.util.UUID;

public final class CatalogoApiMapper {

    private CatalogoApiMapper() {
    }

    public static CrearCondicionVentaCommand toCreateCommand(CondicionVentaRequest request) {
        return new CrearCondicionVentaCommand(
                request.codigo(), request.denominacion(), request.requiereReceta(), request.requiereRetencion(),
                request.fuente(), request.versionFuente(), request.vigenteDesde(), request.vigenteHasta());
    }

    public static ActualizarCondicionVentaCommand toUpdateCommand(CondicionVentaRequest request) {
        return new ActualizarCondicionVentaCommand(
                request.codigo(), request.denominacion(), request.requiereReceta(), request.requiereRetencion(),
                request.fuente(), request.versionFuente(), request.vigenteDesde(), request.vigenteHasta());
    }

    public static CondicionVentaResponse toResponse(CondicionVentaResult result) {
        return new CondicionVentaResponse(
                result.codigo(), result.denominacion(), result.requiereReceta(), result.requiereRetencion(),
                result.fuente(), result.versionFuente(), result.vigenteDesde(), result.vigenteHasta(),
                result.estado());
    }

    public static CrearFormaFarmaceuticaCommand toCreateCommand(FormaFarmaceuticaRequest request) {
        return new CrearFormaFarmaceuticaCommand(request.codigo(), request.denominacion(), request.fuente());
    }

    public static ActualizarFormaFarmaceuticaCommand toUpdateCommand(FormaFarmaceuticaRequest request) {
        return new ActualizarFormaFarmaceuticaCommand(request.codigo(), request.denominacion(), request.fuente());
    }

    public static FormaFarmaceuticaResponse toResponse(FormaFarmaceuticaResult result) {
        return new FormaFarmaceuticaResponse(
                result.codigo(), result.denominacion(), result.fuente(), result.estado());
    }

    public static CrearViaAdministracionCommand toCreateCommand(ViaAdministracionRequest request) {
        return new CrearViaAdministracionCommand(request.codigo(), request.denominacion(), request.fuente());
    }

    public static ActualizarViaAdministracionCommand toUpdateCommand(ViaAdministracionRequest request) {
        return new ActualizarViaAdministracionCommand(request.codigo(), request.denominacion(), request.fuente());
    }

    public static ViaAdministracionResponse toResponse(ViaAdministracionResult result) {
        return new ViaAdministracionResponse(
                result.codigo(), result.denominacion(), result.fuente(), result.estado());
    }

    public static CrearUnidadMedidaCommand toCreateCommand(UnidadMedidaRequest request) {
        return new CrearUnidadMedidaCommand(
                request.codigo(), request.denominacion(), request.simbolo(), request.permiteDecimal(),
                request.fuente());
    }

    public static ActualizarUnidadMedidaCommand toUpdateCommand(UnidadMedidaRequest request) {
        return new ActualizarUnidadMedidaCommand(
                request.codigo(), request.denominacion(), request.simbolo(), request.permiteDecimal(),
                request.fuente());
    }

    public static UnidadMedidaResponse toResponse(UnidadMedidaResult result) {
        return new UnidadMedidaResponse(
                result.codigo(), result.denominacion(), result.simbolo(), result.permiteDecimal(),
                result.fuente(), result.estado());
    }

    public static CrearClasificacionControladaCommand toCreateCommand(ClasificacionControladaRequest request) {
        return new CrearClasificacionControladaCommand(
                request.codigo(), request.denominacion(), request.normaFuente(),
                request.requiereRecetaEspecial(), request.retieneReceta(), request.vigenciaRecetaDias());
    }

    public static ActualizarClasificacionControladaCommand toUpdateCommand(ClasificacionControladaRequest request) {
        return new ActualizarClasificacionControladaCommand(
                request.codigo(), request.denominacion(), request.normaFuente(),
                request.requiereRecetaEspecial(), request.retieneReceta(), request.vigenciaRecetaDias());
    }

    public static ClasificacionControladaResponse toResponse(ClasificacionControladaResult result) {
        return new ClasificacionControladaResponse(
                result.codigo(), result.denominacion(), result.normaFuente(), result.requiereRecetaEspecial(),
                result.retieneReceta(), result.vigenciaRecetaDias(), result.estado());
    }

    public static CrearPrincipioActivoCommand toCreateCommand(PrincipioActivoRequest request) {
        return new CrearPrincipioActivoCommand(
                request.codigoFuente(), request.denominacion(), request.nombreNormalizado(), request.fuente());
    }

    public static ActualizarPrincipioActivoCommand toUpdateCommand(UUID principioActivoId, PrincipioActivoRequest request) {
        return new ActualizarPrincipioActivoCommand(
                principioActivoId, request.codigoFuente(), request.denominacion(), request.nombreNormalizado(),
                request.fuente());
    }

    public static PrincipioActivoResponse toResponse(PrincipioActivoResult result) {
        return new PrincipioActivoResponse(
                result.id(), result.codigoFuente(), result.denominacion(), result.nombreNormalizado(),
                result.fuente(), result.estado());
    }

    public static CrearMarcaCommand toCreateCommand(MarcaRequest request) {
        return new CrearMarcaCommand(request.tenantId(), request.codigo(), request.nombre(), request.descripcion());
    }

    public static ActualizarMarcaCommand toUpdateCommand(UUID marcaId, MarcaRequest request) {
        return new ActualizarMarcaCommand(
                request.tenantId(), marcaId, request.codigo(), request.nombre(), request.descripcion());
    }

    public static MarcaResponse toResponse(MarcaResult result) {
        return new MarcaResponse(
                result.id(), result.tenantId(), result.codigo(), result.nombre(), result.descripcion(),
                result.estado());
    }

    public static CrearCategoriaProductoCommand toCreateCommand(CategoriaProductoRequest request) {
        return new CrearCategoriaProductoCommand(
                request.tenantId(), request.categoriaPadreId(), request.codigo(), request.nombre(),
                request.descripcion(), request.nivel(), request.orden());
    }

    public static ActualizarCategoriaProductoCommand toUpdateCommand(UUID categoriaId, CategoriaProductoRequest request) {
        return new ActualizarCategoriaProductoCommand(
                request.tenantId(), categoriaId, request.categoriaPadreId(), request.codigo(), request.nombre(),
                request.descripcion(), request.nivel(), request.orden());
    }

    public static CategoriaProductoResponse toResponse(CategoriaProductoResult result) {
        return new CategoriaProductoResponse(
                result.id(), result.tenantId(), result.categoriaPadreId(), result.codigo(), result.nombre(),
                result.descripcion(), result.nivel(), result.orden(), result.estado());
    }

    public static CrearProductoReguladoCommand toCreateCommand(ProductoReguladoRequest request) {
        return new CrearProductoReguladoCommand(
                request.tipoProducto(), request.rubroCodigo(), request.tipoRegistro(), request.numeroRegistro(),
                request.denominacion(), request.concentracionTexto(), request.presentacionRegulatoria(),
                request.formaFarmaceuticaCodigo(), request.viaAdministracionCodigo(), request.unidadMedidaCodigo(),
                request.condicionVentaCodigo(), request.clasificacionAtc(), request.clasificacionControladaCodigo(),
                request.tipoLiberacion(), request.origenFabricacion(), request.paisOrigen(),
                request.subpartidaNacional(), request.titularRegistro(), request.fabricante(),
                request.importador(), request.establecimientoExpendio(), request.vigenteDesde(),
                request.vigenteHasta(), request.fuente(), request.versionFuente());
    }

    public static ActualizarProductoReguladoCommand toUpdateCommand(
            UUID productoReguladoId, ProductoReguladoRequest request) {
        return new ActualizarProductoReguladoCommand(
                productoReguladoId, request.tipoProducto(), request.rubroCodigo(), request.tipoRegistro(),
                request.numeroRegistro(), request.denominacion(), request.concentracionTexto(),
                request.presentacionRegulatoria(), request.formaFarmaceuticaCodigo(),
                request.viaAdministracionCodigo(), request.unidadMedidaCodigo(), request.condicionVentaCodigo(),
                request.clasificacionAtc(), request.clasificacionControladaCodigo(), request.tipoLiberacion(),
                request.origenFabricacion(), request.paisOrigen(), request.subpartidaNacional(),
                request.titularRegistro(), request.fabricante(), request.importador(),
                request.establecimientoExpendio(), request.vigenteDesde(), request.vigenteHasta(),
                request.fuente(), request.versionFuente());
    }

    public static AsociarPrincipioActivoCommand toCommand(UUID productoReguladoId, AsociarPrincipioActivoRequest request) {
        return new AsociarPrincipioActivoCommand(
                productoReguladoId, request.principioActivoId(), request.concentracionTexto(),
                request.cantidad(), request.unidadMedidaCodigo(), request.esPrincipal(), request.orden());
    }

    public static ProductoReguladoResponse toResponse(ProductoReguladoResult result) {
        return new ProductoReguladoResponse(
                result.id(), result.tipoProducto(), result.rubroCodigo(), result.tipoRegistro(),
                result.numeroRegistro(), result.denominacion(), result.concentracionTexto(),
                result.presentacionRegulatoria(), result.formaFarmaceuticaCodigo(),
                result.viaAdministracionCodigo(), result.unidadMedidaCodigo(), result.condicionVentaCodigo(),
                result.clasificacionAtc(), result.clasificacionControladaCodigo(), result.tipoLiberacion(),
                result.origenFabricacion(), result.paisOrigen(), result.subpartidaNacional(),
                result.titularRegistro(), result.fabricante(), result.importador(),
                result.establecimientoExpendio(), result.vigenteDesde(), result.vigenteHasta(), result.fuente(),
                result.versionFuente(),
                result.principiosActivos().stream()
                        .map(asociado -> new PrincipioActivoAsociadoResponse(
                                asociado.principioActivoId(), asociado.concentracionTexto(), asociado.cantidad(),
                                asociado.unidadMedidaCodigo(), asociado.esPrincipal(), asociado.orden()))
                        .toList(),
                result.estadoRegulatorio(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<ProductoReguladoResumenResponse> toProductoReguladoPage(
            PaginaResult<ProductoReguladoResumen> result) {
        return new PaginaResponse<>(
                result.items().stream()
                        .map(resumen -> new ProductoReguladoResumenResponse(
                                resumen.id(), resumen.denominacion(), resumen.condicionVentaCodigo(),
                                resumen.estadoRegulatorio()))
                        .toList(),
                result.page(), result.size(), result.totalElements());
    }

    public static CrearSkuCommand toCreateCommand(SkuRequest request, String createdBy) {
        return new CrearSkuCommand(
                request.tenantId(), request.productoReguladoId(), request.categoriaId(), request.marcaId(),
                request.tipoSku(), request.codigoInterno(), request.descripcionComercial(), request.nombreCorto(),
                request.presentacionComercial(), request.unidadVentaCodigo(), request.contenido(),
                request.unidadContenidoCodigo(), request.pesoGramos(), request.altoCm(), request.anchoCm(),
                request.largoCm(), request.permiteVentaFraccion(), request.factorFraccion(),
                request.unidadFraccionCodigo(), request.requiereLote(), request.requiereVencimiento(),
                request.afectoIgv(), request.stockMinimoDefault(), request.stockMaximoDefault(),
                request.imagenUri(), createdBy);
    }

    public static ActualizarSkuCommand toUpdateCommand(UUID skuId, SkuRequest request, String updatedBy) {
        return new ActualizarSkuCommand(
                request.tenantId(), skuId, request.productoReguladoId(), request.categoriaId(), request.marcaId(),
                request.tipoSku(), request.codigoInterno(), request.descripcionComercial(), request.nombreCorto(),
                request.presentacionComercial(), request.unidadVentaCodigo(), request.contenido(),
                request.unidadContenidoCodigo(), request.pesoGramos(), request.altoCm(), request.anchoCm(),
                request.largoCm(), request.permiteVentaFraccion(), request.factorFraccion(),
                request.unidadFraccionCodigo(), request.requiereLote(), request.requiereVencimiento(),
                request.afectoIgv(), request.stockMinimoDefault(), request.stockMaximoDefault(),
                request.imagenUri(), updatedBy);
    }

    public static AgregarCodigoBarraCommand toCommand(UUID tenantId, UUID skuId, AgregarCodigoBarraRequest request) {
        return new AgregarCodigoBarraCommand(
                tenantId, skuId, request.codigoBarra(), request.tipoCodigo(), request.vigenteDesde(),
                request.vigenteHasta());
    }

    public static SkuResponse toResponse(SkuResult result) {
        return new SkuResponse(
                result.id(), result.tenantId(), result.productoReguladoId(), result.categoriaId(),
                result.marcaId(), result.tipoSku(), result.codigoInterno(), result.descripcionComercial(),
                result.nombreCorto(), result.presentacionComercial(), result.unidadVentaCodigo(),
                result.contenido(), result.unidadContenidoCodigo(), result.pesoGramos(), result.altoCm(),
                result.anchoCm(), result.largoCm(), result.permiteVentaFraccion(), result.factorFraccion(),
                result.unidadFraccionCodigo(), result.requiereLote(), result.requiereVencimiento(),
                result.afectoIgv(), result.stockMinimoDefault(), result.stockMaximoDefault(), result.imagenUri(),
                result.codigosBarra().stream()
                        .map(codigo -> new CodigoBarraSkuResponse(
                                codigo.codigoBarra(), codigo.tipoCodigo(), codigo.esPrincipal(),
                                codigo.vigenteDesde(), codigo.vigenteHasta(), codigo.estado()))
                        .toList(),
                result.estado(), result.createdBy(), result.createdAt(), result.updatedBy(), result.updatedAt());
    }

    public static PaginaResponse<SkuResumenResponse> toSkuPage(PaginaResult<SkuResumen> result) {
        return new PaginaResponse<>(
                result.items().stream()
                        .map(resumen -> new SkuResumenResponse(
                                resumen.id(), resumen.codigoInterno(), resumen.descripcionComercial(),
                                resumen.tipoSku(), resumen.estado()))
                        .toList(),
                result.page(), result.size(), result.totalElements());
    }
}
