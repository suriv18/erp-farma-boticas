package com.softprimesolutions.compras.infrastructure.client;

import com.softprimesolutions.compras.application.error.ComprasErrors;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort;
import com.softprimesolutions.inventario.api.IngresoCompraSolicitud;
import com.softprimesolutions.inventario.api.IngresoInventarioApi;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import org.springframework.stereotype.Component;

@Component
public class InventarioIngresoAdapter implements IngresoInventarioPort {

    private final IngresoInventarioApi inventario;

    public InventarioIngresoAdapter(IngresoInventarioApi inventario) {
        this.inventario = inventario;
    }

    @Override
    public Result<IngresoRegistrado, ApplicationError> ingresar(IngresoSolicitado solicitud) {
        return inventario.registrarIngresoCompra(new IngresoCompraSolicitud(
                        solicitud.tenantId(), solicitud.almacenId(), solicitud.skuId(), solicitud.numeroLote(),
                        solicitud.fechaVencimiento(), solicitud.cantidad(), solicitud.recepcionId(),
                        solicitud.recepcionLineaId(), solicitud.proveedorId(), solicitud.actorId(),
                        solicitud.idempotencyKey()))
                .fold(
                        movimiento -> Result.<IngresoRegistrado, ApplicationError>success(new IngresoRegistrado(
                                movimiento.movimientoId(), movimiento.loteId(), movimiento.stockPosterior())),
                        error -> Result.failure(IngresoInventarioApi.CODIGO_CONCURRENCIA.equals(error.code())
                                ? ComprasErrors.modificacionConcurrente()
                                : error));
    }
}
