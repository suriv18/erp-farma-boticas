package com.softprimesolutions.compras.api.mapper;

import com.softprimesolutions.compras.api.dto.request.ItemRecepcionRequest;
import com.softprimesolutions.compras.api.dto.request.LineaOrdenCompraRequest;
import com.softprimesolutions.compras.api.dto.request.OrdenCompraRequest;
import com.softprimesolutions.compras.api.dto.request.ProveedorRequest;
import com.softprimesolutions.compras.api.dto.request.RecepcionRequest;
import com.softprimesolutions.compras.api.dto.response.LineaOrdenCompraResponse;
import com.softprimesolutions.compras.api.dto.response.LineaRecepcionResponse;
import com.softprimesolutions.compras.api.dto.response.OrdenCompraResponse;
import com.softprimesolutions.compras.api.dto.response.OrdenCompraResumenResponse;
import com.softprimesolutions.compras.api.dto.response.PaginaResponse;
import com.softprimesolutions.compras.api.dto.response.ProveedorResponse;
import com.softprimesolutions.compras.api.dto.response.RecepcionResponse;
import com.softprimesolutions.compras.application.dto.command.CrearOrdenCompraCommand;
import com.softprimesolutions.compras.application.dto.command.ItemRecepcionInput;
import com.softprimesolutions.compras.application.dto.command.LineaOrdenCompraInput;
import com.softprimesolutions.compras.application.dto.command.ProveedorInput;
import com.softprimesolutions.compras.application.dto.command.RegistrarRecepcionCommand;
import com.softprimesolutions.compras.application.dto.result.LineaOrdenCompraResult;
import com.softprimesolutions.compras.application.dto.result.LineaRecepcionResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResult;
import com.softprimesolutions.compras.application.dto.result.OrdenCompraResumenResult;
import com.softprimesolutions.compras.application.dto.result.PaginaResult;
import com.softprimesolutions.compras.application.dto.result.ProveedorResult;
import com.softprimesolutions.compras.application.dto.result.RecepcionResult;
import java.util.UUID;
import java.util.function.Function;

public final class ComprasApiMapper {

    private ComprasApiMapper() {
    }

    public static ProveedorInput toInput(ProveedorRequest request) {
        return new ProveedorInput(
                request.tipoDocumento(), request.numeroDocumento(), request.razonSocial(),
                request.nombreComercial(), request.direccion(), request.ubigeo(), request.telefono(),
                request.email(), request.contactoNombre(), request.contactoTelefono(), request.contactoEmail(),
                request.condicionPagoDefault(), request.diasCreditoDefault(), request.monedaDefault(),
                request.esLaboratorio(), request.esImportador(), request.esDistribuidor(), request.calificacion());
    }

    public static CrearOrdenCompraCommand toCommand(UUID tenantId, UUID actorId, OrdenCompraRequest request) {
        return new CrearOrdenCompraCommand(
                tenantId, actorId, request.proveedorId(), request.establecimientoDestinoId(),
                request.fechaEntregaEstimada(), request.moneda(), request.tipoCambio(), request.condicionPago(),
                request.diasCredito(), request.observacion(),
                request.lineas().stream().map(ComprasApiMapper::toInput).toList());
    }

    public static RegistrarRecepcionCommand toCommand(
            UUID tenantId, UUID actorId, String idempotencyKey, RecepcionRequest request) {
        return new RegistrarRecepcionCommand(
                tenantId, actorId, idempotencyKey, request.ordenCompraId(), request.almacenId(),
                request.documentoProveedorTipo(), request.documentoProveedorSerie(),
                request.documentoProveedorNumero(), request.guiaRemisionRemitente(),
                request.guiaRemisionTransportista(), request.temperaturaRecepcionC(), request.humedadRelativaPct(),
                request.observacion(), request.items().stream().map(ComprasApiMapper::toInput).toList());
    }

    public static ProveedorResponse toResponse(ProveedorResult result) {
        return new ProveedorResponse(
                result.id(), result.tipoDocumento(), result.numeroDocumento(), result.razonSocial(),
                result.nombreComercial(), result.direccion(), result.ubigeo(), result.telefono(), result.email(),
                result.contactoNombre(), result.contactoTelefono(), result.contactoEmail(),
                result.condicionPagoDefault(), result.diasCreditoDefault(), result.monedaDefault(),
                result.esLaboratorio(), result.esImportador(), result.esDistribuidor(), result.calificacion(),
                result.estado());
    }

    public static OrdenCompraResponse toResponse(OrdenCompraResult result) {
        return new OrdenCompraResponse(
                result.id(), result.numero(), result.proveedorId(), result.establecimientoDestinoId(),
                result.fechaEmision(), result.fechaEntregaEstimada(), result.moneda(), result.tipoCambio(),
                result.condicionPago(), result.diasCredito(), result.subtotal(), result.descuentoTotal(),
                result.impuestoTotal(), result.total(), result.estado(), result.observacion(), result.aprobadoAt(),
                result.lineas().stream().map(ComprasApiMapper::toResponse).toList());
    }

    public static RecepcionResponse toResponse(RecepcionResult result) {
        return new RecepcionResponse(
                result.id(), result.numero(), result.ordenCompraId(), result.proveedorId(),
                result.establecimientoId(), result.almacenId(), result.documentoProveedorTipo(),
                result.documentoProveedorSerie(), result.documentoProveedorNumero(),
                result.guiaRemisionRemitente(), result.guiaRemisionTransportista(), result.fechaRecepcion(),
                result.temperaturaRecepcionC(), result.humedadRelativaPct(), result.estado(), result.observacion(),
                result.lineas().stream().map(ComprasApiMapper::toResponse).toList());
    }

    public static PaginaResponse<ProveedorResponse> toProveedorPage(PaginaResult<ProveedorResult> page) {
        return toPage(page, ComprasApiMapper::toResponse);
    }

    public static PaginaResponse<OrdenCompraResumenResponse> toOrdenPage(PaginaResult<OrdenCompraResumenResult> page) {
        return toPage(page, ComprasApiMapper::toResponse);
    }

    public static PaginaResponse<RecepcionResponse> toRecepcionPage(PaginaResult<RecepcionResult> page) {
        return toPage(page, ComprasApiMapper::toResponse);
    }

    private static <S, T> PaginaResponse<T> toPage(PaginaResult<S> page, Function<S, T> mapper) {
        return new PaginaResponse<>(
                page.items().stream().map(mapper).toList(), page.page(), page.size(), page.totalElements());
    }

    private static LineaOrdenCompraInput toInput(LineaOrdenCompraRequest request) {
        return new LineaOrdenCompraInput(
                request.skuId(), request.cantidad(), request.unidadMedidaCodigo(), request.precioUnitario(),
                request.descuento(), request.impuesto(), request.toleranciaExcesoPct(),
                request.toleranciaDefectoPct());
    }

    private static ItemRecepcionInput toInput(ItemRecepcionRequest request) {
        return new ItemRecepcionInput(
                request.numeroLineaOrden(), request.numeroLote(), request.fechaFabricacion(),
                request.fechaVencimiento(), request.cantidadRecibida(), request.cantidadRechazada(),
                request.motivoRechazo(), request.costoUnitario(), request.observacion());
    }

    private static OrdenCompraResumenResponse toResponse(OrdenCompraResumenResult result) {
        return new OrdenCompraResumenResponse(
                result.id(), result.numero(), result.proveedorId(), result.proveedorRazonSocial(),
                result.establecimientoDestinoId(), result.fechaEmision(), result.fechaEntregaEstimada(),
                result.moneda(), result.total(), result.estado());
    }

    private static LineaOrdenCompraResponse toResponse(LineaOrdenCompraResult result) {
        return new LineaOrdenCompraResponse(
                result.numeroLinea(), result.skuId(), result.descripcion(), result.cantidad(),
                result.unidadMedidaCodigo(), result.precioUnitario(), result.descuento(), result.impuesto(),
                result.totalLinea(), result.toleranciaExcesoPct(), result.toleranciaDefectoPct(),
                result.cantidadRecibida(), result.cantidadPendiente());
    }

    private static LineaRecepcionResponse toResponse(LineaRecepcionResult result) {
        return new LineaRecepcionResponse(
                result.id(), result.numeroLinea(), result.numeroLineaOrden(), result.skuId(), result.numeroLote(),
                result.fechaFabricacion(), result.fechaVencimiento(), result.cantidadRecibida(),
                result.cantidadAceptada(), result.cantidadRechazada(), result.costoUnitario(),
                result.decisionCalidad(), result.motivoDecision(), result.observacion(), result.loteId());
    }
}
