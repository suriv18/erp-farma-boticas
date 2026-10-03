package com.softprimesolutions.ventas.api.mapper;

import com.softprimesolutions.ventas.api.dto.request.AbrirTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.CerrarTurnoRequest;
import com.softprimesolutions.ventas.api.dto.request.LineaVentaRequest;
import com.softprimesolutions.ventas.api.dto.request.VentaRequest;
import com.softprimesolutions.ventas.api.dto.response.LineaVentaResponse;
import com.softprimesolutions.ventas.api.dto.response.LoteConsumidoResponse;
import com.softprimesolutions.ventas.api.dto.response.PagoResponse;
import com.softprimesolutions.ventas.api.dto.response.PaginaResponse;
import com.softprimesolutions.ventas.api.dto.response.TurnoResponse;
import com.softprimesolutions.ventas.api.dto.response.VentaResponse;
import com.softprimesolutions.ventas.api.dto.response.VentaResumenResponse;
import com.softprimesolutions.ventas.application.dto.command.AbrirTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.CerrarTurnoCommand;
import com.softprimesolutions.ventas.application.dto.command.LineaVentaInput;
import com.softprimesolutions.ventas.application.dto.command.RegistrarVentaCommand;
import com.softprimesolutions.ventas.application.dto.result.LineaVentaResult;
import com.softprimesolutions.ventas.application.dto.result.LoteConsumidoResult;
import com.softprimesolutions.ventas.application.dto.result.PaginaResult;
import com.softprimesolutions.ventas.application.dto.result.PagoResult;
import com.softprimesolutions.ventas.application.dto.result.TurnoResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResult;
import com.softprimesolutions.ventas.application.dto.result.VentaResumenResult;
import java.util.UUID;

public final class VentasApiMapper {

    private VentasApiMapper() {
    }

    public static AbrirTurnoCommand toCommand(UUID tenantId, UUID actorId, AbrirTurnoRequest request) {
        return new AbrirTurnoCommand(tenantId, actorId, request.terminalId(), request.fondoInicial());
    }

    public static CerrarTurnoCommand toCommand(
            UUID tenantId, UUID actorId, UUID turnoId, CerrarTurnoRequest request) {
        return new CerrarTurnoCommand(
                tenantId, actorId, turnoId, request.totalDeclarado(), request.observacion());
    }

    public static RegistrarVentaCommand toCommand(
            UUID tenantId, UUID actorId, String idempotencyKey, VentaRequest request) {
        return new RegistrarVentaCommand(
                tenantId, actorId, idempotencyKey, request.terminalId(), request.almacenId(),
                request.lineas().stream().map(VentasApiMapper::toInput).toList(),
                request.pago().montoRecibido());
    }

    public static TurnoResponse toResponse(TurnoResult result) {
        return new TurnoResponse(
                result.id(), result.terminalId(), result.establecimientoId(), result.cajeroId(),
                result.aperturaAt(), result.fondoInicial(), result.estado(), result.cierreAt(),
                result.totalVentasSistema(), result.totalSistema(), result.totalDeclarado(), result.diferencia(),
                result.observacionCierre());
    }

    public static VentaResponse toResponse(VentaResult result) {
        return new VentaResponse(
                result.id(), result.numeroOperacion(), result.terminalId(), result.turnoId(),
                result.establecimientoId(), result.vendedorId(), result.fechaVenta(), result.moneda(),
                result.subtotal(), result.descuentoTotal(), result.impuestoTotal(), result.total(), result.estado(),
                result.lineas().stream().map(VentasApiMapper::toResponse).toList(), toResponse(result.pago()));
    }

    public static PaginaResponse<VentaResumenResponse> toResponse(PaginaResult<VentaResumenResult> pagina) {
        return new PaginaResponse<>(
                pagina.items().stream().map(VentasApiMapper::toResponse).toList(), pagina.page(), pagina.size(),
                pagina.totalElements());
    }

    private static LineaVentaInput toInput(LineaVentaRequest request) {
        return new LineaVentaInput(request.skuId(), request.cantidad(), request.precioUnitario());
    }

    private static LineaVentaResponse toResponse(LineaVentaResult result) {
        return new LineaVentaResponse(
                result.numeroLinea(), result.skuId(), result.descripcion(), result.unidadVentaCodigo(),
                result.cantidad(), result.precioUnitario(), result.totalLinea(),
                result.lotes().stream().map(VentasApiMapper::toResponse).toList());
    }

    private static LoteConsumidoResponse toResponse(LoteConsumidoResult result) {
        return new LoteConsumidoResponse(result.loteId(), result.cantidad());
    }

    private static PagoResponse toResponse(PagoResult result) {
        return new PagoResponse(result.medioPago(), result.monto(), result.montoRecibido(), result.vuelto());
    }

    private static VentaResumenResponse toResponse(VentaResumenResult result) {
        return new VentaResumenResponse(
                result.id(), result.numeroOperacion(), result.terminalId(), result.fechaVenta(), result.total(),
                result.estado());
    }
}
