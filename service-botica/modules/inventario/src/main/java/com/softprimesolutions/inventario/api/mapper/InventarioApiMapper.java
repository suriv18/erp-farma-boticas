package com.softprimesolutions.inventario.api.mapper;

import com.softprimesolutions.inventario.api.dto.request.BloquearLoteRequest;
import com.softprimesolutions.inventario.api.dto.request.RegistrarMovimientoRequest;
import com.softprimesolutions.inventario.api.dto.response.LoteResponse;
import com.softprimesolutions.inventario.api.dto.response.MovimientoResponse;
import com.softprimesolutions.inventario.api.dto.response.PaginaResponse;
import com.softprimesolutions.inventario.api.dto.response.PosicionResponse;
import com.softprimesolutions.inventario.application.dto.command.BloquearLoteCommand;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.dto.result.MovimientoResult;
import com.softprimesolutions.inventario.application.dto.result.PaginaResult;
import com.softprimesolutions.inventario.application.dto.result.PosicionResult;
import java.util.UUID;

public final class InventarioApiMapper {

    private InventarioApiMapper() {
    }

    public static BloquearLoteCommand toCommand(
            UUID tenantId, UUID loteId, UUID actorId, BloquearLoteRequest request) {
        return new BloquearLoteCommand(tenantId, loteId, request.motivo(), actorId);
    }

    public static RegistrarMovimientoCommand toCommand(
            UUID tenantId, UUID actorId, String idempotencyKey, RegistrarMovimientoRequest request) {
        return new RegistrarMovimientoCommand(
                tenantId, request.almacenId(), request.skuId(), request.loteId(), request.numeroLote(),
                request.fechaVencimiento(), request.tipo(), request.cantidad(), request.motivo(), actorId,
                idempotencyKey);
    }

    public static LoteResponse toResponse(LoteResult result) {
        return new LoteResponse(
                result.id(), result.skuId(), result.numeroLote(), result.fechaVencimiento(), result.estado(),
                result.motivoEstado(), result.bloqueadoAt(), result.vendible());
    }

    public static MovimientoResponse toResponse(MovimientoResult result) {
        return new MovimientoResponse(
                result.id(), result.posicionId(), result.loteId(), result.tipo(), result.naturaleza(),
                result.cantidad(), result.stockAnterior(), result.stockPosterior(), result.fechaNegocio());
    }

    public static PaginaResponse<PosicionResponse> toPosicionPage(PaginaResult<PosicionResult> page) {
        return new PaginaResponse<>(
                page.items().stream().map(InventarioApiMapper::toResponse).toList(),
                page.page(), page.size(), page.totalElements());
    }

    private static PosicionResponse toResponse(PosicionResult result) {
        return new PosicionResponse(
                result.id(), result.establecimientoId(), result.almacenId(), result.skuId(), result.loteId(),
                result.numeroLote(), result.fechaVencimiento(), result.estadoLote(), result.estadoInventario(),
                result.cantidadFisica(), result.cantidadReservada(), result.cantidadDisponible(),
                result.vendible(), result.version(), result.ultimoMovimientoAt());
    }
}
