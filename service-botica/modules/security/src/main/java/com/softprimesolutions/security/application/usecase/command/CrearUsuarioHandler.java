package com.softprimesolutions.security.application.usecase.command;

import com.softprimesolutions.security.application.dto.command.CrearUsuarioCommand;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.mapper.IamApplicationMapper;
import com.softprimesolutions.security.application.port.in.CrearUsuarioUseCase;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
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
        var identidadId = new IdentidadId(identifierGenerator.next());
        var identidad = Identidad.register(
                identidadId, command.documentType(), command.documentNumber(), command.firstNames(),
                command.lastNames(), command.username(), command.email(), command.phone(), clock.now());
        return identidad.fold(
                value -> registerUsuario(value, command),
                this::validationFailure);
    }

    private Result<UsuarioResult, ApplicationError> registerUsuario(Identidad identidad, CrearUsuarioCommand command) {
        var user = Usuario.register(
                new UsuarioId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                identidad.id(),
                command.displayName(),
                command.credentialChangeRequired(),
                command.mfaRequired(),
                clock.now());
        return user.fold(usuario -> persist(identidad, usuario), this::validationFailure);
    }

    private Result<UsuarioResult, ApplicationError> persist(Identidad identidad, Usuario user) {
        return switch (writePort.save(identidad, user)) {
            case CREATED -> Result.success(IamApplicationMapper.toResult(identidad, user));
            case TENANT_NOT_FOUND -> Result.failure(new StandardApplicationError(
                    "SEC_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND));
            case DUPLICATE_EMAIL -> Result.failure(new StandardApplicationError(
                    "SEC_EMAIL_DUPLICADO", "El correo electrónico ya está registrado.",
                    ErrorCategory.CONFLICT, Map.of("email", identidad.email())));
            case DUPLICATE_USERNAME -> Result.failure(new StandardApplicationError(
                    "SEC_USERNAME_DUPLICADO", "El username ya está registrado.", ErrorCategory.CONFLICT));
            case DUPLICATE_DOCUMENT -> Result.failure(new StandardApplicationError(
                    "SEC_DOCUMENTO_DUPLICADO", "El documento ya está registrado.", ErrorCategory.CONFLICT));
            case DUPLICATE_CONSTRAINT -> Result.failure(new StandardApplicationError(
                    "SEC_USUARIO_DUPLICADO",
                    "El correo, username o documento ya pertenecen a otra identidad.",
                    ErrorCategory.CONFLICT));
        };
    }

    private Result<UsuarioResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
