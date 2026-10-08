package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarUnidadMedidaCommand;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.UnidadMedida;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarUnidadMedidaHandler implements ActualizarUnidadMedidaUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarUnidadMedidaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<UnidadMedidaResult, ApplicationError> execute(ActualizarUnidadMedidaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var unidad = UnidadMedida.create(
                command.codigo(), command.denominacion(), command.simbolo(), command.permiteDecimal(),
                command.fuente());
        return unidad.fold(this::persist, this::validationFailure);
    }

    private Result<UnidadMedidaResult, ApplicationError> persist(UnidadMedida unidad) {
        var outcome = writePort.save(unidad);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_UNIDAD_MEDIDA_NO_ENCONTRADA", "La unidad de medida indicada no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(unidad));
    }

    private Result<UnidadMedidaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
