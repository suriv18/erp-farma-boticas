package com.softprimesolutions.organizacion.application.port.out;

import com.softprimesolutions.organizacion.application.dto.result.AlcancePuesto;
import java.util.UUID;

@FunctionalInterface
public interface AlcancePuestoPort {
    AlcancePuesto alcanceDe(UUID tenantId, UUID usuarioId);
}
