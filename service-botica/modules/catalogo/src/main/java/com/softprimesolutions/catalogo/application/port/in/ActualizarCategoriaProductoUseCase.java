package com.softprimesolutions.catalogo.application.port.in;

import com.softprimesolutions.catalogo.application.dto.command.ActualizarCategoriaProductoCommand;
import com.softprimesolutions.catalogo.application.dto.result.CategoriaProductoResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarCategoriaProductoUseCase {
    Result<CategoriaProductoResult, ApplicationError> execute(ActualizarCategoriaProductoCommand command);
}
