package com.softprimesolutions.clientes.application.usecase.query;

import com.softprimesolutions.clientes.application.dto.query.ObtenerClienteQuery;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.port.in.ObtenerClienteUseCase;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ObtenerClienteHandler implements ObtenerClienteUseCase {

    private final ClientesReadPort readPort;

    public ObtenerClienteHandler(ClientesReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<ClienteResult, ApplicationError> execute(ObtenerClienteQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        return readPort.findClienteById(query.tenantId(), query.clienteId())
                .<Result<ClienteResult, ApplicationError>>map(Result::success)
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CLI_CLIENTE_NO_ENCONTRADO", "El cliente indicado no existe.", ErrorCategory.NOT_FOUND)));
    }
}
