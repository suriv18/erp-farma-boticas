package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarMarcaQuery;
import com.softprimesolutions.catalogo.application.dto.result.MarcaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarMarcaUseCase {
    Result<MarcaResult, ApplicationError> execute(ConsultarMarcaQuery query);
}
