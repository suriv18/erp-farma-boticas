package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarRubroComercialQuery(UUID tenantId, UUID rubroComercialId)
        implements Query<RubroComercialResult> {
}
