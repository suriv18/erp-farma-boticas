package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarTipoDocumentoIdentidadQuery;
import com.softprimesolutions.catalogo.application.dto.result.TipoDocumentoIdentidadResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarTipoDocumentoIdentidadUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarTipoDocumentoIdentidadHandler implements ConsultarTipoDocumentoIdentidadUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarTipoDocumentoIdentidadHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<TipoDocumentoIdentidadResult, ApplicationError> execute(ConsultarTipoDocumentoIdentidadQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findTipoDocumentoIdentidadByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<TipoDocumentoIdentidadResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_TIPO_DOCUMENTO_IDENTIDAD_NO_ENCONTRADO", "El tipo de documento indicado no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
