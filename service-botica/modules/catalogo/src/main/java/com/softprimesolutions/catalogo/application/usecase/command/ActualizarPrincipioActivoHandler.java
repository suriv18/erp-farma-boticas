package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.PrincipioActivoResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.PrincipioActivo;
import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarPrincipioActivoHandler implements ActualizarPrincipioActivoUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarPrincipioActivoHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<PrincipioActivoResult, ApplicationError> execute(ActualizarPrincipioActivoCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var principioActivo = PrincipioActivo.create(
                new PrincipioActivoId(command.principioActivoId()), command.codigoFuente(), command.denominacion(),
                command.nombreNormalizado(), command.fuente());
        return principioActivo.fold(this::persist, this::validationFailure);
    }

    private Result<PrincipioActivoResult, ApplicationError> persist(PrincipioActivo principioActivo) {
        var outcome = writePort.save(principioActivo);
        if (outcome == CatalogoSoportePort.SavePrincipioActivoOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_PRINCIPIO_ACTIVO_NO_ENCONTRADO", "El principio activo indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(principioActivo));
    }

    private Result<PrincipioActivoResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
