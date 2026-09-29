package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CrearEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CrearEmpresaOperadoraUseCase {
    Result<EmpresaOperadoraResult, ApplicationError> execute(CrearEmpresaOperadoraCommand command);
}
