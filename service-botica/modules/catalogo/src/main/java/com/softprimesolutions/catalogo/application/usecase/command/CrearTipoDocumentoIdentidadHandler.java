package com.softprimesolutions.catalogo.application.usecase.command;

import com.softprimesolutions.catalogo.application.dto.command.CrearTipoDocumentoIdentidadCommand;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.CrearTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.domain.model.soporte.TipoDocumentoIdentidad;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class CrearTipoDocumentoIdentidadHandler implements CrearTipoDocumentoIdentidadUseCase {

    private final CatalogoSoportePort writePort;

    public CrearTipoDocumentoIdentidadHandler(CatalogoSoportePort writePort) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    @Override
    public Result<TipoDocumentoIdentidadResult, ApplicationError> execute(CrearTipoDocumentoIdentidadCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var tipoDocumentoIdentidad = TipoDocumentoIdentidad.create(
                command.codigo(), command.sigla(), command.denominacion(), command.max(), command.min());
        return tipoDocumentoIdentidad.fold(this::persist, this::validationFailure);
    }

    private Result<TipoDocumentoIdentidadResult, ApplicationError> persist(TipoDocumentoIdentidad tipoDocumentoIdentidad) {
        var outcome = writePort.save(tipoDocumentoIdentidad);
        if (outcome == CatalogoSoportePort.SaveOutcome.DUPLICATE_CODIGO) {
            return Result.failure(new StandardApplicationError(
                    "CAT_TIPO_DOCUMENTO_IDENTIDAD_DUPLICADO", "Ya existe un tipo de documento con el código indicado.",
                    ErrorCategory.CONFLICT));
        }
        return Result.success(CatalogoApplicationMapper.toResult(tipoDocumentoIdentidad));
    }

    private Result<TipoDocumentoIdentidadResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
