package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarMarcaQuery;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarMarcaHandler implements ConsultarMarcaUseCase {

    private final CatalogoComercialPort comercialPort;

    public ConsultarMarcaHandler(CatalogoComercialPort comercialPort) {
        this.comercialPort = Objects.requireNonNull(comercialPort, "comercialPort es obligatorio");
    }

    @Override
    public Result<MarcaResult, ApplicationError> execute(ConsultarMarcaQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return comercialPort.findMarcaById(query.tenantId(), query.marcaId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<MarcaResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_MARCA_NO_ENCONTRADA", "La marca indicada no existe.", ErrorCategory.NOT_FOUND)));
    }
}
