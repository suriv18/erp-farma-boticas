package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface SalidaInventarioPort {

    Result<List<LoteConsumo>, ApplicationError> descontar(SalidaSolicitada solicitud);

    record SalidaSolicitada(
            UUID tenantId,
            UUID almacenId,
            UUID skuId,
            BigDecimal cantidad,
            UUID ventaId,
            UUID ventaLineaId,
            UUID actorId,
            String idempotencyKey) {
    }
}
