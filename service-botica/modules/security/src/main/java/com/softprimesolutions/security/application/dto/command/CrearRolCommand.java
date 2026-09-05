package com.softprimesolutions.security.application.dto.command;

import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.util.UUID;

public record CrearRolCommand(
        UUID tenantId, String code, String name, String description, String roleType, boolean systemRole)
        implements Command<RolResult> {
}
