package com.softprimesolutions.security.application.dto.command;

import com.softprimesolutions.security.application.dto.result.AsignacionRolResult;
import com.softprimesolutions.shared.application.cqrs.Command;
import java.time.Instant;
import java.util.UUID;

public record AsignarRolUsuarioCommand(
        UUID tenantId,
        UUID userId,
        UUID roleId,
        String scopeType,
        UUID companyId,
        UUID establishmentId,
        UUID warehouseId,
        UUID terminalId,
        Instant validFrom,
        Instant validUntil,
        String createdBy) implements Command<AsignacionRolResult> {
}
