package com.softprimesolutions.clientes.application.port.in;

import com.softprimesolutions.clientes.application.dto.command.CambiarEstadoClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface CambiarEstadoClienteUseCase {

    Result<ClienteResult, ApplicationError> execute(CambiarEstadoClienteCommand command);
}
