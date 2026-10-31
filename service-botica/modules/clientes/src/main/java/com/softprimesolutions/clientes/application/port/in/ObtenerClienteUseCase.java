package com.softprimesolutions.clientes.application.port.in;

import com.softprimesolutions.clientes.application.dto.query.ObtenerClienteQuery;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ObtenerClienteUseCase {

    Result<ClienteResult, ApplicationError> execute(ObtenerClienteQuery query);
}
