package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearPrincipioActivoHandler implements CrearPrincipioActivoUseCase {

    private final CatalogoSoportePort writePort;
    private final IdentifierGenerator identifierGenerator;

    public CrearPrincipioActivoHandler(CatalogoSoportePort writePort, IdentifierGenerator identifierGenerator) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
    }

    @Override
    public Result<PrincipioActivoResult, ApplicationError> execute(CrearPrincipioActivoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var principioActivo = PrincipioActivo.create(
                new PrincipioActivoId(identifierGenerator.next()), command.codigoFuente(), command.denominacion(),
                command.nombreNormalizado(), command.fuente());
        return principioActivo.fold(this::persist, this::validationFailure);
    }

    private Result<PrincipioActivoResult, ApplicationError> persist(PrincipioActivo principioActivo) {
        var outcome = writePort.save(principioActivo);
        if (outcome == CatalogoSoportePort.SavePrincipioActivoOutcome.DUPLICATE_DENOMINACION) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRINCIPIO_ACTIVO_DUPLICADO", "Ya existe un principio activo con la denominación indicada.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(principioActivo));
    }

    private Result<PrincipioActivoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
