package com.softprimesolutions.security.application.dto.command;

import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.Set;
import java.util.UUID;

public record ReemplazarPermisosRolCommand(UUID roleId, Set<String> permissionCodes, String grantedBy)
        implements Command<RolResult> {

    public ReemplazarPermisosRolCommand {
        permissionCodes = permissionCodes == null ? null : Set.copyOf(permissionCodes);
    }
}
