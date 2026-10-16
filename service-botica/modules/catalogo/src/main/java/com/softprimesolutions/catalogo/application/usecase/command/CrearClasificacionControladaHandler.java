package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearClasificacionControladaCommand;
import com.softprimesolutions.catalogo.application.dto.result.ClasificacionControladaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.ClasificacionControlada;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearClasificacionControladaHandler implements CrearClasificacionControladaUseCase {

    private final CatalogoSoportePort writePort;

    public CrearClasificacionControladaHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<ClasificacionControladaResult, ApplicationError> execute(
            CrearClasificacionControladaCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var clasificacion = ClasificacionControlada.create(
                command.codigo(), command.denominacion(), command.normaFuente(),
                command.requiereRecetaEspecial(), command.retieneReceta(), command.vigenciaRecetaDias());
        return clasificacion.fold(this::persist, this::validationFailure);
    }

    private Result<ClasificacionControladaResult, ApplicationError> persist(ClasificacionControlada clasificacion) {
        var outcome = writePort.save(clasificacion);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_CLASIFICACION_CONTROLADA_DUPLICADA",
                    "Ya existe una clasificación controlada con el código indicado.", ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(clasificacion));
    }

    private Result<ClasificacionControladaResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
