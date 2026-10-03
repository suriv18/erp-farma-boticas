package com.softprimesolutions.ventas.infrastructure.client;

import com.softprimesolutions.inventario.api.SalidaInventarioApi;
import com.softprimesolutions.inventario.api.SalidaVentaSolicitud;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class InventarioSalidaAdapter implements SalidaInventarioPort {

    private final SalidaInventarioApi inventario;

    public InventarioSalidaAdapter(SalidaInventarioApi inventario) {
        this.inventario = inventario;
    }

    @Override
    public Result<List<LoteConsumo>, ApplicationError> descontar(SalidaSolicitada solicitud) {
        return inventario.registrarSalidaVenta(new SalidaVentaSolicitud(
                        solicitud.tenantId(), solicitud.almacenId(), solicitud.skuId(), solicitud.cantidad(),
                        solicitud.ventaId(), solicitud.ventaLineaId(), solicitud.actorId(),
                        solicitud.idempotencyKey()))
                .fold(
                        registrada -> Result.<List<LoteConsumo>, ApplicationError>success(registrada.lotes().stream()
                                .map(lote -> new LoteConsumo(lote.loteId(), lote.cantidad()))
                                .toList()),
                        error -> Result.failure(SalidaInventarioApi.CODIGO_CONCURRENCIA.equals(error.code())
                                ? VentasErrors.modificacionConcurrente()
                                : error));
    }
}
