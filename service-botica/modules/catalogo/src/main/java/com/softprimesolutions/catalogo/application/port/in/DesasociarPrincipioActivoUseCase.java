package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.DesasociarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface DesasociarPrincipioActivoUseCase {
    Result<ProductoReguladoResult, ApplicationError> execute(DesasociarPrincipioActivoCommand command);
}
