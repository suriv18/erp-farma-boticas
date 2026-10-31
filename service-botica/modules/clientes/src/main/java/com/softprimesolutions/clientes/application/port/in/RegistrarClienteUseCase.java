package com.softprimesolutions.clientes.application.port.in;

import com.softprimesolutions.clientes.application.dto.command.RegistrarClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.RegistroClienteResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface RegistrarClienteUseCase {

    Result<RegistroClienteResult, ApplicationError> execute(RegistrarClienteCommand command);
}
