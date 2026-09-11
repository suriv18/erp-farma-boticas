package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarSkuQuery;
import com.softprimesolutions.catalogo.application.dto.result.SkuResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarSkuUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarSkuHandler implements ConsultarSkuUseCase {

    private final CatalogoComercialPort comercialPort;

    public ConsultarSkuHandler(CatalogoComercialPort comercialPort) {
        this.comercialPort = Objects.requireNonNull(comercialPort, "comercialPort es obligatorio");
    }

    @Override
    public Result<SkuResult, ApplicationError> execute(ConsultarSkuQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return comercialPort.findSkuById(query.tenantId(), query.skuId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<SkuResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_SKU_NO_ENCONTRADO", "El SKU indicado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
