package com.softprimesolutions.security.application.dto.query;

import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.shared.application.cqrs.Query;
import java.util.UUID;

public record ListarUsuariosQuery(UUID tenantId, String search, int page, int size)
        implements Query<PaginaResult<UsuarioResult>> {
}
