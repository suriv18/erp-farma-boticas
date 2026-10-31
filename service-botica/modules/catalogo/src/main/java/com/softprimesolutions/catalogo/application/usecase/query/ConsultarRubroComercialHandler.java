package com.softprimesolutions.catalogo.application.usecase.query;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarRubroComercialQuery;
import com.softprimesolutions.catalogo.application.dto.result.RubroComercialResult;
import com.softprimesolutions.catalogo.application.mapper.CatalogoApplicationMapper;
import com.softprimesolutions.catalogo.application.port.in.ConsultarRubroComercialUseCase;
import com.softprimesolutions.catalogo.application.port.out.RubroComercialPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ConsultarRubroComercialHandler implements ConsultarRubroComercialUseCase {

    private final RubroComercialPort rubroComercialPort;

    public ConsultarRubroComercialHandler(RubroComercialPort rubroComercialPort) {
        this.rubroComercialPort = Objects.requireNonNull(rubroComercialPort, "rubroComercialPort es obligatorio");
    }

    @Override
    public Result<RubroComercialResult, ApplicationError> execute(ConsultarRubroComercialQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return rubroComercialPort.findById(query.tenantId(), query.rubroComercialId())
                .map(CatalogoApplicationMapper::toResult)
                .map(Result::<RubroComercialResult, ApplicationError>success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CAT_RUBRO_COMERCIAL_NO_ENCONTRADO", "El rubro comercial no existe en el tenant indicado.",
                        ErrorCategory.NOT_FOUND)));
    }
}
