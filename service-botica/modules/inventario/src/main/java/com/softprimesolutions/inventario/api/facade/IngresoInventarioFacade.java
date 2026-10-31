package com.softprimesolutions.inventario.api.facade;

import com.softprimesolutions.inventario.api.IngresoCompraRegistrado;
import com.softprimesolutions.inventario.api.IngresoCompraSolicitud;
import com.softprimesolutions.inventario.api.IngresoInventarioApi;
import com.softprimesolutions.inventario.application.dto.command.DocumentoOrigen;
import com.softprimesolutions.inventario.application.dto.command.RegistrarMovimientoCommand;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.domain.model.TipoMovimiento;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class IngresoInventarioFacade implements IngresoInventarioApi {

    static final String DOCUMENTO_RECEPCION_COMPRA = "RECEPCION_COMPRA";
    static final String MOTIVO_RECEPCION_COMPRA = "Recepcion de compra";

    private final RegistrarMovimientoUseCase registrarMovimiento;

    public IngresoInventarioFacade(RegistrarMovimientoUseCase registrarMovimiento) {
        this.registrarMovimiento = Objects.requireNonNull(registrarMovimiento, "registrarMovimiento es obligatorio");
    }

    @Override
    public Result<IngresoCompraRegistrado, ApplicationError> registrarIngresoCompra(
            IngresoCompraSolicitud solicitud) {
        Objects.requireNonNull(solicitud, "solicitud es obligatoria");
        var origen = new DocumentoOrigen(
                DOCUMENTO_RECEPCION_COMPRA, solicitud.recepcionId(), solicitud.recepcionLineaId(),
                solicitud.proveedorId());
        var command = new RegistrarMovimientoCommand(
                solicitud.tenantId(), solicitud.almacenId(), solicitud.skuId(), null, solicitud.numeroLote(),
                solicitud.fechaVencimiento(), TipoMovimiento.INGRESO_COMPRA.name(), solicitud.cantidad(),
                MOTIVO_RECEPCION_COMPRA, solicitud.actorId(), solicitud.idempotencyKey(), origen);
        return registrarMovimiento.execute(command).map(movimiento -> new IngresoCompraRegistrado(
                movimiento.id(), movimiento.loteId(), movimiento.posicionId(), movimiento.stockAnterior(),
                movimiento.stockPosterior()));
    }
}
