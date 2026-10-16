package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearViaAdministracionCommand;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearViaAdministracionHandler implements CrearViaAdministracionUseCase {

    private final CatalogoSoportePort writePort;

    public CrearViaAdministracionHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ViaAdministracionResult, ApplicationError> execute(CrearViaAdministracionCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var via = ViaAdministracion.create(command.codigo(), command.denominacion(), command.fuente());
        return via.fold(this::persist, this::validationFailure);
    }

    private Result<ViaAdministracionResult, ApplicationError> persist(ViaAdministracion via) {
        var outcome = writePort.save(via);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_VIA_ADMINISTRACION_DUPLICADA", "Ya existe una vía de administración con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(via));
    }

    private Result<ViaAdministracionResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
