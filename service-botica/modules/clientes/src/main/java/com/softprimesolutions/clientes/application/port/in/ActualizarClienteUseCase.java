package com.softprimesolutions.clientes.application.port.in;

import com.softprimesolutions.clientes.application.dto.command.ActualizarClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ActualizarClienteUseCase {

    Result<ClienteResult, ApplicationError> execute(ActualizarClienteCommand command);
}
