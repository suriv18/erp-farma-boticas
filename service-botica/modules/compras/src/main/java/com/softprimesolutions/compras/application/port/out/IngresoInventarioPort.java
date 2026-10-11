package com.softprimesolutions.compras.application.port.out;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface IngresoInventarioPort {

    Result<IngresoRegistrado, ApplicationError> ingresar(IngresoSolicitado solicitud);

    record IngresoSolicitado(
            UUID tenantId,
            UUID almacenId,
            UUID skuId,
            String numeroLote,
            LocalDate fechaVencimiento,
            BigDecimal cantidad,
            UUID recepcionId,
            UUID recepcionLineaId,
            UUID proveedorId,
            UUID actorId,
            String idempotencyKey) {
    }

    record IngresoRegistrado(UUID movimientoId, UUID loteId, BigDecimal stockPosterior) {
    }
}
