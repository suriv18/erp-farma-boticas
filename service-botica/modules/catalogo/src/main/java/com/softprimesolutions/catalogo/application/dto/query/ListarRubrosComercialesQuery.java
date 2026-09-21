package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarRubrosComercialesQuery(
        UUID tenantId, String texto, Boolean esFarmaceutico, String estado, int page, int size)
        implements Query<PaginaResult<RubroComercialResult>> {
}
