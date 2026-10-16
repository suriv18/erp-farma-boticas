package com.softprimesolutions.security.application.port.out;

import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import java.util.Optional;
import java.util.UUID;

public interface IamReadPort {

    PaginaResult<UsuarioResult> findUsers(UUID tenantId, String search, int page, int size);

    PaginaResult<RolResult> findRoles(UUID tenantId, String search, int page, int size);

    PaginaResult<PermisoResult> findPermissions(String search, int page, int size);

    Optional<RolResult> findRoleById(UUID tenantId, UUID roleId);
}
