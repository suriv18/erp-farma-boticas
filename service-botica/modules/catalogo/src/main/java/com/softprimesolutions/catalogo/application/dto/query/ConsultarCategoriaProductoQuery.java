package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ConsultarCategoriaProductoQuery(UUID tenantId, UUID categoriaId)
        implements Query<CategoriaProductoResult> {
}
