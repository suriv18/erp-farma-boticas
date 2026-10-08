package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarCondicionVentaQuery;
import com.softprimesolutions.catalogo.application.dto.result.CondicionVentaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarCondicionVentaUseCase {
    Result<CondicionVentaResult, ApplicationError> execute(ConsultarCondicionVentaQuery query);
}
