package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarMarcaQuery(UUID tenantId, UUID marcaId) implements Query<MarcaResult> {
}
