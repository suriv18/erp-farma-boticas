package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.catalogo.application.dto.result.SkuResumen;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarSkusQuery(
        UUID tenantId, String texto, UUID categoriaId, UUID marcaId, String tipoSku, String estado,
        int page, int size) implements Query<PaginaResult<SkuResumen>> {
}
