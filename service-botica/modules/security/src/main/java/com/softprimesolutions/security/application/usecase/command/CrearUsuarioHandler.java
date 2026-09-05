package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.dto.command.CrearUsuarioCommand;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.mapper.IamApplicationMapper;
import com.softprimesolutions.security.application.port.in.CrearUsuarioUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class CrearUsuarioHandler implements CrearUsuarioUseCase {

    private final IamWritePort writePort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public CrearUsuarioHandler(IamWritePort writePort, IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<UsuarioResult, ApplicationError> execute(CrearUsuarioCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var user = Usuario.register(
                new UsuarioId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                command.identityProvider(),
                command.identityIssuer(),
                command.identitySubject(),
                command.emailClaim(),
                command.documentType(),
                command.documentNumber(),
                command.firstNames(),
                command.lastNames(),
                command.username(),
                command.email(),
                command.phone(),
                command.displayName(),
                command.credentialChangeRequired(),
                command.mfaRequired(),
                clock.now());
        return user.fold(this::persist, this::validationFailure);
    }

    private Result<UsuarioResult, ApplicationError> persist(Usuario user) {
        return switch (writePort.save(user)) {
            case CREATED -> Result.success(IamApplicationMapper.toResult(user));
            case TENANT_NOT_FOUND -> Result.failure(new StandardApplicationError(
                    "SEC_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
            case DUPLICATE_IDENTITY -> Result.failure(new StandardApplicationError(
                    "SEC_IDENTIDAD_DUPLICADA",
                    "La identidad externa ya está vinculada a otro usuario.",
                    ErrorCategory.CONFLICT));
            case DUPLICATE_EMAIL -> Result.failure(new StandardApplicationError(
                    "SEC_EMAIL_DUPLICADO",
                    "El correo electrónico ya está registrado.",
                    ErrorCategory.CONFLICT,
                    Map.of("email", user.email())));
            case DUPLICATE_USERNAME -> Result.failure(new StandardApplicationError(
                    "SEC_USERNAME_DUPLICADO", "El username ya está registrado en el tenant.", ErrorCategory.CONFLICT));
            case DUPLICATE_DOCUMENT -> Result.failure(new StandardApplicationError(
                    "SEC_DOCUMENTO_DUPLICADO", "El documento ya está registrado en el tenant.", ErrorCategory.CONFLICT));
            case DUPLICATE_CONSTRAINT -> Result.failure(new StandardApplicationError(
                    "SEC_USUARIO_DUPLICADO",
                    "La identidad externa o el correo ya pertenecen a otro usuario.",
                    ErrorCategory.CONFLICT));
        };
    }

    private Result<UsuarioResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
