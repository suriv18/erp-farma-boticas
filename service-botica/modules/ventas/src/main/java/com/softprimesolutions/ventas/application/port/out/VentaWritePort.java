package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import com.softprimesolutions.ventas.domain.model.Venta;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VentaWritePort {

    Optional<VentaExistente> findPorIdempotencia(UUID tenantId, String idempotencyKey);

    GuardadoOutcome insertar(Venta venta, String idempotencyKey, String huella);

    void registrarLotes(UUID tenantId, UUID ventaLineaId, List<LoteConsumo> lotes);

    record VentaExistente(UUID id, String huella) {
    }
}
