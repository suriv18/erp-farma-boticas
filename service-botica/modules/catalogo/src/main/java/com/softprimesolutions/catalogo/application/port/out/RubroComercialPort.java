package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.RubroComercial;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface RubroComercialPort {

    SaveOutcome save(RubroComercial rubroComercial);

    Optional<RubroComercial> findById(UUID tenantId, UUID rubroComercialId);

    boolean changeStatus(UUID tenantId, UUID rubroComercialId, String status, Instant changedAt);

    enum SaveOutcome { CREATED, UPDATED, DUPLICATE_CODIGO, TENANT_NOT_FOUND, NOT_FOUND }
}
