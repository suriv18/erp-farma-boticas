package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarViaAdministracionCommand;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.ViaAdministracion;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarViaAdministracionHandler implements ActualizarViaAdministracionUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarViaAdministracionHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ViaAdministracionResult, ApplicationError> execute(ActualizarViaAdministracionCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var via = ViaAdministracion.create(command.codigo(), command.denominacion(), command.fuente());
        return via.fold(this::persist, this::validationFailure);
    }

    private Result<ViaAdministracionResult, ApplicationError> persist(ViaAdministracion via) {
        var outcome = writePort.save(via);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_VIA_ADMINISTRACION_NO_ENCONTRADA", "La vía de administración indicada no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(via));
    }

    private Result<ViaAdministracionResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
