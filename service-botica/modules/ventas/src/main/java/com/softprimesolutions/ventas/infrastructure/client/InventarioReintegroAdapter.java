package com.softprimesolutions.ventas.infrastructure.client;

import com.softprimesolutions.inventario.api.AnulacionInventarioApi;
import com.softprimesolutions.inventario.api.ReintegroVentaSolicitud;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.application.error.VentasErrors;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class InventarioReintegroAdapter implements ReintegroInventarioPort {

    private final AnulacionInventarioApi inventario;

    public InventarioReintegroAdapter(AnulacionInventarioApi inventario) {
        this.inventario = inventario;
    }

    @Override
    public Result<List<LoteConsumo>, ApplicationError> reintegrar(UUID tenantId, UUID ventaId, UUID actorId) {
        return inventario.reintegrarSalidasDeVenta(new ReintegroVentaSolicitud(tenantId, ventaId, actorId))
                .fold(
                        registrado -> Result.<List<LoteConsumo>, ApplicationError>success(registrado.movimientos()
                                .stream()
                                .map(movimiento -> new LoteConsumo(movimiento.loteId(), movimiento.cantidad()))
                                .toList()),
                        error -> Result.failure(AnulacionInventarioApi.CODIGO_CONCURRENCIA.equals(error.code())
                                ? VentasErrors.modificacionConcurrente()
                                : error));
    }
}
