package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarViaAdministracionQuery;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarViaAdministracionHandler implements ConsultarViaAdministracionUseCase {

    private final CatalogoSoportePort soportePort;

    public ConsultarViaAdministracionHandler(CatalogoSoportePort soportePort) {
        this.soportePort = Objects.requireNonNull(soportePort, "soportePort es obligatorio");
    }

    @Override
    public Result<ViaAdministracionResult, ApplicationError> execute(ConsultarViaAdministracionQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return soportePort.findViaAdministracionByCodigo(query.codigo())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<ViaAdministracionResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_VIA_ADMINISTRACION_NO_ENCONTRADA", "La vía de administración indicada no existe.",
                        ErrorCategory.NOT_FOUND)));
    }
}
