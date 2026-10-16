package com.softprimesolutions.inventario.api.facade;

import com.softprimesolutions.inventario.api.LoteConsumido;
import com.softprimesolutions.inventario.api.SalidaInventarioApi;
import com.softprimesolutions.inventario.api.SalidaVentaRegistrada;
import com.softprimesolutions.inventario.api.SalidaVentaSolicitud;
import com.softprimesolutions.inventario.application.dto.command.RegistrarSalidaVentaCommand;
import com.softprimesolutions.inventario.application.port.in.RegistrarSalidaVentaUseCase;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class SalidaInventarioFacade implements SalidaInventarioApi {

    private final RegistrarSalidaVentaUseCase registrarSalidaVenta;

    public SalidaInventarioFacade(RegistrarSalidaVentaUseCase registrarSalidaVenta) {
        this.registrarSalidaVenta = Objects.requireNonNull(registrarSalidaVenta, "registrarSalidaVenta es obligatorio");
    }

    @Override
    public Result<SalidaVentaRegistrada, ApplicationError> registrarSalidaVenta(SalidaVentaSolicitud solicitud) {
        Objects.requireNonNull(solicitud, "solicitud es obligatoria");
        var command = new RegistrarSalidaVentaCommand(
                solicitud.tenantId(), solicitud.almacenId(), solicitud.skuId(), solicitud.cantidad(),
                solicitud.ventaId(), solicitud.ventaLineaId(), solicitud.actorId(), solicitud.idempotencyKey());
        return registrarSalidaVenta.execute(command).map(movimientos -> new SalidaVentaRegistrada(
                movimientos.stream().map(movimiento -> new LoteConsumido(
                        movimiento.id(), movimiento.loteId(), movimiento.cantidad(), movimiento.stockPosterior()))
                        .toList()));
    }
}
