package com.softprimesolutions.security.application.dto.query;

import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.shared.application.cqrs.Query;

public record ListarPermisosQuery(String search, int page, int size)
        implements Query<PaginaResult<PermisoResult>> {
}
