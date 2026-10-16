package com.softprimesolutions.inventario.application.port.out;

import com.softprimesolutions.inventario.application.dto.result.LoteResult;
import com.softprimesolutions.inventario.application.dto.result.PaginaResult;
import com.softprimesolutions.inventario.application.dto.result.PosicionResult;
import java.util.Optional;
import java.util.UUID;

public interface InventarioReadPort {

    PaginaResult<PosicionResult> findPosiciones(
            UUID tenantId, UUID establecimientoId, UUID almacenId, UUID skuId, int page, int size);

    Optional<LoteResult> findLoteById(UUID tenantId, UUID loteId);
}
