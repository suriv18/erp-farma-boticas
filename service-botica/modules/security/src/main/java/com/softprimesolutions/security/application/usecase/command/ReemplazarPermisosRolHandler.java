package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.dto.command.ReemplazarPermisosRolCommand;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.mapper.IamApplicationMapper;
import com.softprimesolutions.security.application.port.in.ReemplazarPermisosRolUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import com.softprimesolutions.shared.application.port.ClockPort;
import java.util.Map;
import java.util.Objects;

public final class ReemplazarPermisosRolHandler implements ReemplazarPermisosRolUseCase {

    private final IamWritePort writePort;
    private final ClockPort clock;

    public ReemplazarPermisosRolHandler(IamWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<RolResult, ApplicationError> execute(ReemplazarPermisosRolCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        if (command.roleId() == null || command.permissionCodes() == null) {
            return validation("roleId", "El rol y la colección de permisos son obligatorios.");
        }
        var role = writePort.findRole(command.roleId());
        if (role.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (!writePort.allPermissionsExist(command.permissionCodes())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_PERMISO_DESCONOCIDO",
                    "Uno o más permisos no pertenecen al catálogo.",
                    ErrorCategory.VALIDATION,
                    Map.of("permissionCodes", command.permissionCodes())));
        }
        if (command.grantedBy() == null || command.grantedBy().isBlank() || command.grantedBy().length() > 100) {
            return validation("grantedBy", "El actor que concede permisos es obligatorio.");
        }
        return role.get().replacePermissions(command.permissionCodes(), clock.now())
                .fold(updatedRole -> persist(updatedRole, command.grantedBy()), this::validationFailure);
    }

    private Result<RolResult, ApplicationError> persist(
            com.softprimesolutions.security.domain.model.Rol role, String grantedBy) {
        writePort.replacePermissions(role, grantedBy, clock.now());
        return Result.success(IamApplicationMapper.toResult(role));
    }

    private Result<RolResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }

    private static Result<RolResult, ApplicationError> validation(String field, String message) {
        return Result.failure(new StandardApplicationError(
                "SEC_ROL_INVALIDO", message, ErrorCategory.VALIDATION, Map.of("field", field)));
    }
}
