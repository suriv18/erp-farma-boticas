package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.query.ConsultarViaAdministracionQuery;
import com.softprimesolutions.catalogo.application.dto.result.ViaAdministracionResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ConsultarViaAdministracionUseCase {
    Result<ViaAdministracionResult, ApplicationError> execute(ConsultarViaAdministracionQuery query);
}
