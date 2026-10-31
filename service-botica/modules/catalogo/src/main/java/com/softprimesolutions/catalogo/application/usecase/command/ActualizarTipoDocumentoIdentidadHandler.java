package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ActualizarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarTipoDocumentoIdentidadHandler implements ActualizarTipoDocumentoIdentidadUseCase {

    private final CatalogoSoportePort writePort;

    public ActualizarTipoDocumentoIdentidadHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ActualizarTipoDocumentoIdentidadCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var tipoDocumentoIdentidad = TipoDocumentoIdentidad.create(
                command.codigo(), command.sigla(), command.denominacion(), command.max(), command.min());
        return tipoDocumentoIdentidad.fold(this::persist, this::validationFailure);
    }

    private Result<TipoDocumentoIdentidadResult, ApplicationError> persist(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
        var outcome = writePort.save(tipoDocumentoIdentidad);
        if (outcome == CatalogoSoportePort.SaveOutcome.NOT_FOUND) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", "El tipo de documento indicado no existe.",
                    ErrorCategory.NOT_FOUND));
        }
        return Result.success(CatalogoApplicationMapper.toResult(tipoDocumentoIdentidad));
    }

    private Result<TipoDocumentoIdentidadResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
