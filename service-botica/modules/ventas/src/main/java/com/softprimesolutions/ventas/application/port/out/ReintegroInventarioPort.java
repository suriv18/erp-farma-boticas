package com.softprimesolutions.ventas.application.port.out;

import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.ventas.domain.model.LoteConsumo;
import java.util.List;
import java.util.UUID;

@FunctionalInterface
public interface ReintegroInventarioPort {

    Result<List<LoteConsumo>, ApplicationError> reintegrar(UUID tenantId, UUID ventaId, UUID actorId);
}
