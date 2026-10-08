package com.softprimesolutions.inventario.api.facade;

import com.softprimesolutions.inventario.api.AnulacionInventarioApi;
import com.softprimesolutions.inventario.api.MovimientoReintegrado;
import com.softprimesolutions.inventario.api.ReintegroVentaRegistrado;
import com.softprimesolutions.inventario.api.ReintegroVentaSolicitud;
import com.softprimesolutions.inventario.application.dto.command.ReintegrarSalidasDeVentaCommand;
import com.softprimesolutions.inventario.application.port.in.ReintegrarSalidasDeVentaUseCase;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class AnulacionInventarioFacade implements AnulacionInventarioApi {

    private final ReintegrarSalidasDeVentaUseCase reintegrarSalidasDeVenta;

    public AnulacionInventarioFacade(ReintegrarSalidasDeVentaUseCase reintegrarSalidasDeVenta) {
        this.reintegrarSalidasDeVenta =
                Objects.requireNonNull(reintegrarSalidasDeVenta, "reintegrarSalidasDeVenta es obligatorio");
    }

    @Override
    public Result<ReintegroVentaRegistrado, ApplicationError> reintegrarSalidasDeVenta(
            ReintegroVentaSolicitud solicitud) {
        Objects.requireNonNull(solicitud, "solicitud es obligatoria");
        var command = new ReintegrarSalidasDeVentaCommand(
                solicitud.tenantId(), solicitud.ventaId(), solicitud.actorId());
        return reintegrarSalidasDeVenta.execute(command).map(movimientos -> new ReintegroVentaRegistrado(
                movimientos.stream().map(movimiento -> new MovimientoReintegrado(
                        movimiento.id(), movimiento.loteId(), movimiento.cantidad(), movimiento.stockPosterior()))
                        .toList()));
    }
}
