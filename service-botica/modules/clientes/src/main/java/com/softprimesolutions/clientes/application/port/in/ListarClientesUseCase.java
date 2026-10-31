package com.softprimesolutions.clientes.application.port.in;

import com.softprimesolutions.clientes.application.dto.query.ListarClientesQuery;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.PaginaResult;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;

@FunctionalInterface
public interface ListarClientesUseCase {

    Result<PaginaResult<ClienteResult>, ApplicationError> execute(ListarClientesQuery query);
}
