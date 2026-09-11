package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.AsociarPrincipioActivoCommand;
import com.softprimesolutions.catalogo.application.dto.result.ProductoReguladoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface AsociarPrincipioActivoUseCase {
    Result<ProductoReguladoResult, ApplicationError> execute(AsociarPrincipioActivoCommand command);
}
