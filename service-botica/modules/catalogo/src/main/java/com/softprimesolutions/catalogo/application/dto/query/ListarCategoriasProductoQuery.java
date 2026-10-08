package com.softprimesolutions.catalogo.application.dto.query;

import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.catalogo.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarCategoriasProductoQuery(
        UUID tenantId, String texto, UUID categoriaPadreId, String estado, int page, int size)
        implements Query<PaginaResult<CategoriaProductoResult>> {
}
