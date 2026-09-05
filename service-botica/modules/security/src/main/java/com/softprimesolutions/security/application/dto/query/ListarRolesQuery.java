package com.softprimesolutions.security.application.dto.query;

import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarRolesQuery(UUID tenantId, String search, int page, int size)
        implements Query<PaginaResult<RolResult>> {
}
