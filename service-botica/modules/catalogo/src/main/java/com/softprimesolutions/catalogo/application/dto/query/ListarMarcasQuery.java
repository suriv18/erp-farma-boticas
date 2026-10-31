package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarMarcasQuery(UUID tenantId, String texto, String estado, int page, int size)
        implements Query<PaginaResult<MarcaResult>> {
}
