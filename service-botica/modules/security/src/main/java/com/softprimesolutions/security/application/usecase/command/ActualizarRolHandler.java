package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.dto.command.ActualizarRolCommand;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.mapper.IamApplicationMapper;
import com.softprimesolutions.security.application.port.in.ActualizarRolUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Objects;

public final class ActualizarRolHandler implements ActualizarRolUseCase {

    private final IamWritePort writePort;
    private final ClockPort clock;

    public ActualizarRolHandler(IamWritePort writePort, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<RolResult, ApplicationError> execute(ActualizarRolCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        if (!writePort.roleBelongsToTenant(command.roleId(), command.tenantId())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        var role = writePort.findRole(command.roleId());
        if (role.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        var existingRole = role.get();
        var normalizedCode = command.code() == null ? null : command.code().trim().toUpperCase(Locale.ROOT);
        if (normalizedCode != null && !normalizedCode.equals(existingRole.code())
                && writePort.existsActiveRoleWithCode(
                        existingRole.tenantId().value(), normalizedCode, command.roleId())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_CODIGO_DUPLICADO", "Ya existe un rol con el código indicado.", ErrorCategory.CONFLICT));
        }
        return existingRole
                .updateDetails(command.code(), command.name(), command.description(), command.roleType(), clock.now())
                .fold(this::persist, this::validationFailure);
    }

    private Result<RolResult, ApplicationError> persist(Rol role) {
        var outcome = writePort.save(role);
        if (outcome == IamWritePort.SaveRolOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "SEC_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == IamWritePort.SaveRolOutcome.DUPLICATE_CODE) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_CODIGO_DUPLICADO", "Ya existe un rol con el código indicado.", ErrorCategory.CONFLICT));
        }
        return Result.success(IamApplicationMapper.toResult(role));
    }

    private Result<RolResult, ApplicationError> validationFailure(ErrorDetail error) {
        var category = "SEC_ROL_SISTEMA_NO_EDITABLE".equals(error.code())
                ? ErrorCategory.CONFLICT
                : ErrorCategory.VALIDATION;
        return Result.failure(new StandardApplicationError(error.code(), error.message(), category, error.metadata()));
    }
}
