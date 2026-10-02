package com.softprimesolutions.compras.application.port.out;

import com.softprimesolutions.compras.domain.model.Recepcion;
import java.util.Optional;
import java.util.UUID;

public interface RecepcionWritePort {

    Optional<RecepcionExistente> findPorBusinessUuid(UUID tenantId, UUID businessUuid);

    GuardadoOutcome insertar(Recepcion recepcion, UUID businessUuid, String huella);

    record RecepcionExistente(UUID id, String huella) {
    }
}
