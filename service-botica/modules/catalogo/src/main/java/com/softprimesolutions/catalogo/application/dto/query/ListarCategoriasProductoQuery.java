package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.List;
import java.util.UUID;

public record ListarCategoriasProductoQuery(UUID tenantId, UUID categoriaPadreId, String estado)
        implements Query<List<CategoriaProductoResult>> {
}
