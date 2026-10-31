package com.softprimesolutions.clientes.application.usecase.query;

import com.softprimesolutions.clientes.application.dto.query.ListarClientesQuery;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.dto.result.PaginaResult;
import com.softprimesolutions.clientes.application.port.in.ListarClientesUseCase;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class ListarClientesHandler implements ListarClientesUseCase {

    private final ClientesReadPort readPort;

    public ListarClientesHandler(ClientesReadPort readPort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
    }

    @Override
    public Result<PaginaResult<ClienteResult>, ApplicationError> execute(ListarClientesQuery query) {
        Objects.requireNonNull(query, "query es obligatorio");
        if (query.page() < 0 || query.size() < 1 || query.size() > 100) {
            return Result.failure(new StandardApplicationError(
                    "CLI_PAGINACION_INVALIDA",
                    "page debe ser mayor o igual a 0 y size debe estar entre 1 y 100.",
                    ErrorCategory.VALIDATION,
                    Map.of("page", query.page(), "size", query.size())));
        }
        return Result.success(readPort.findClientes(query.tenantId(), query.search(), query.page(), query.size()));
    }
}
