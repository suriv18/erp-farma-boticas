package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;
import java.util.UUID;

public record ListarMarcasQuery(UUID tenantId, String estado) implements Query<List<MarcaResult>> {
}
