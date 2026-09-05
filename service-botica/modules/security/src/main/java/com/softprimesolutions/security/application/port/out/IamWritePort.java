package com.softprimesolutions.security.application.port.out;

import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface IamWritePort {

    SaveUsuarioOutcome save(Usuario user);

    SaveRolOutcome save(Rol role);

    SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt);

    Optional<Rol> findRole(UUID roleId);

    boolean allPermissionsExist(Set<String> permissionCodes);

    boolean tenantExists(UUID tenantId);

    boolean userBelongsToTenant(UUID userId, UUID tenantId);

    boolean roleBelongsToTenant(UUID roleId, UUID tenantId);

    boolean scopeExists(UUID tenantId, AmbitoOrganizacional scope);

    SaveAssignmentOutcome save(AsignacionRol assignment);

    enum SaveUsuarioOutcome {
        CREATED,
        TENANT_NOT_FOUND,
        DUPLICATE_IDENTITY,
        DUPLICATE_USERNAME,
        DUPLICATE_EMAIL,
        DUPLICATE_DOCUMENT,
        DUPLICATE_CONSTRAINT
    }
    enum SaveRolOutcome { CREATED, UPDATED, TENANT_NOT_FOUND, DUPLICATE_CODE }
    enum SaveAssignmentOutcome { CREATED, DUPLICATE }
}
