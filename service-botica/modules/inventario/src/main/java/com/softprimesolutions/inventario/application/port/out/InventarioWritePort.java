package com.softprimesolutions.inventario.application.port.out;

import com.softprimesolutions.inventario.domain.model.EstadoLote;
import com.softprimesolutions.inventario.domain.model.Lote;
import com.softprimesolutions.inventario.domain.model.PosicionInventario;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

public interface InventarioWritePort {

    Optional<Lote> findLote(UUID tenantId, UUID loteId);

    Optional<Lote> findLotePorClave(UUID tenantId, UUID skuId, String numeroLote, LocalDate fechaVencimiento);

    Optional<PosicionInventario> findPosicion(UUID tenantId, UUID almacenId, UUID loteId);

    Optional<MovimientoRegistrado> findMovimientoPorBusinessUuid(UUID tenantId, UUID businessUuid);

    boolean actualizarEstado(Lote lote, EstadoLote estadoPrevio);

    RegistroOutcome registrar(RegistroMovimiento registro);

    enum RegistroOutcome {
        REGISTRADO,
        MODIFICACION_CONCURRENTE
    }
}
