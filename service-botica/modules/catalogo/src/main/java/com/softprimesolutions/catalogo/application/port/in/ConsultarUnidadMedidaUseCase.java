package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarUnidadMedidaQuery;
import com.softprimesolutions.catalogo.application.dto.result.UnidadMedidaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarUnidadMedidaUseCase {
    Result<UnidadMedidaResult, ApplicationError> execute(ConsultarUnidadMedidaQuery query);
}
