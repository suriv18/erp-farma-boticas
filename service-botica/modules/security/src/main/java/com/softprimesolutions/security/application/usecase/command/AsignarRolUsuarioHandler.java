package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.dto.command.AsignarRolUsuarioCommand;
import com.softprimesolutions.security.application.dto.result.AsignacionRolResult;
import com.softprimesolutions.security.application.mapper.IamApplicationMapper;
import com.softprimesolutions.security.application.port.in.AsignarRolUsuarioUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.valueobject.TipoAmbito;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class AsignarRolUsuarioHandler implements AsignarRolUsuarioUseCase {

    private final IamWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public AsignarRolUsuarioHandler(IamWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<AsignacionRolResult, ApplicationError> execute(AsignarRolUsuarioCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        if (command.tenantId() == null || !writePort.tenantExists(command.tenantId())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (command.userId() == null || !writePort.userBelongsToTenant(command.userId(), command.tenantId())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_USUARIO_NO_ENCONTRADO", "El usuario indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (command.roleId() == null || !writePort.roleBelongsToTenant(command.roleId(), command.tenantId())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_NO_ENCONTRADO", "El rol indicado no existe.", ErrorCategory.NOT_FOUND));
        }

        final TipoAmbito scopeType;
        try {
            scopeType = TipoAmbito.valueOf(command.scopeType() == null
                    ? "" : command.scopeType().trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return Result.failure(new StandardApplicationError(
                    "SEC_AMBITO_INVALIDO",
                    "El tipo de ámbito no es válido.",
                    ErrorCategory.VALIDATION,
                    Map.of("field", "scopeType")));
        }

        var now = clock.now();
        var assignment = AsignacionRol.create(
                identifierGenerator.next(), command.tenantId(), command.userId(), command.roleId(), scopeType,
                command.companyId(), command.establishmentId(), command.warehouseId(), command.terminalId(),
                command.validFrom() == null ? now : command.validFrom(), command.validUntil(),
                command.createdBy(), now);
        return assignment.fold(this::validateScopeAndPersist, this::validationFailure);
    }

    private Result<AsignacionRolResult, ApplicationError> validateScopeAndPersist(AsignacionRol assignment) {
        if (!writePort.scopeExists(assignment.tenantId().value(), assignment.scope())) {
            return Result.failure(new StandardApplicationError(
                    "SEC_AMBITO_NO_ENCONTRADO",
                    "El ámbito organizacional no existe o no pertenece al tenant.",
                    ErrorCategory.NOT_FOUND));
        }
        return persist(assignment);
    }

    private Result<AsignacionRolResult, ApplicationError> persist(AsignacionRol assignment) {
        if (writePort.save(assignment) == IamWritePort.SaveAssignmentOutcome.DUPLICATE) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ASIGNACION_DUPLICADA",
                    "El usuario ya posee ese rol en el ámbito indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(IamApplicationMapper.toResult(assignment));
    }

    private Result<AsignacionRolResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
