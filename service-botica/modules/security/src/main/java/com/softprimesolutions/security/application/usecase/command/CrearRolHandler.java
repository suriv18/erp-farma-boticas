package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.dto.command.CrearRolCommand;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.mapper.IamApplicationMapper;
import com.softprimesolutions.security.application.port.in.CrearRolUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearRolHandler implements CrearRolUseCase {

    private final IamWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearRolHandler(IamWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<RolResult, ApplicationError> execute(CrearRolCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var role = Rol.create(
                new RolId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.code(), command.name(), command.description(), command.roleType(),
                command.systemRole(), clock.now());
        return role.fold(this::persist, this::validationFailure);
    }

    private Result<RolResult, ApplicationError> persist(Rol role) {
        var outcome = writePort.save(role);
        if (outcome == IamWritePort.SaveRolOutcome.TENANT_NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "SEC_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
        }
        if (outcome == IamWritePort.SaveRolOutcome.DUPLICATE_CODE) {
            return Result.failure(new StandardApplicationError(
                    "SEC_ROL_DUPLICADO", "Ya existe un rol con el código indicado.", ErrorCategory.CONFLICT));
        }
        return Result.success(IamApplicationMapper.toResult(role));
    }

    private Result<RolResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
