package com.softprimesolutions.organizacion.application.port.in;

import com.softprimesolutions.organizacion.application.dto.command.CambiarEstadoEmpresaCommand;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CambiarEstadoEmpresaUseCase {

    Result<EmpresaOperadoraResult, ApplicationError> execute(CambiarEstadoEmpresaCommand command);
}
