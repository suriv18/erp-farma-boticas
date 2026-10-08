package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearFormaFarmaceuticaCommand;
import com.softprimesolutions.catalogo.application.dto.result.FormaFarmaceuticaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.FormaFarmaceutica;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearFormaFarmaceuticaHandler implements CrearFormaFarmaceuticaUseCase {

    private final CatalogoSoportePort writePort;

    public CrearFormaFarmaceuticaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<FormaFarmaceuticaResult, ApplicationError> execute(CrearFormaFarmaceuticaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var forma = FormaFarmaceutica.create(command.codigo(), command.denominacion(), command.fuente());
        return forma.fold(this::persist, this::validationFailure);
    }

    private Result<FormaFarmaceuticaResult, ApplicationError> persist(FormaFarmaceutica forma) {
        var outcome = writePort.save(forma);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_FORMA_FARMACEUTICA_DUPLICADA", "Ya existe una forma farmacéutica con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(forma));
    }

    private Result<FormaFarmaceuticaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
