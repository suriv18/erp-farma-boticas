package com.softprimesolutions.clientes.application.usecase.command;

import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.mapper.ClientesApplicationMapper;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort.UpdateClienteOutcome;
import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;

final class ModificarCliente {

    private final ClientesReadPort readPort;
    private final ClientesWritePort writePort;

    ModificarCliente(ClientesReadPort readPort, ClientesWritePort writePort) {
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
    }

    Result<ClienteResult, ApplicationError> execute(
            UUID tenantId, UUID clienteId, Function<Cliente, Result<Cliente, ErrorDetail>> cambio) {
        return readPort.findClienteById(tenantId, clienteId)
                .map(ClientesApplicationMapper::toDomain)
                .map(cambio)
                .map(modificado -> modificado.fold(this::persist, ModificarCliente::validationFailure))
                .orElseGet(ModificarCliente::notFound);
    }

    private Result<ClienteResult, ApplicationError> persist(Cliente cliente) {
        return writePort.update(cliente) == UpdateClienteOutcome.UPDATED
                ? Result.success(ClientesApplicationMapper.toResult(cliente))
                : notFound();
    }

    private static Result<ClienteResult, ApplicationError> notFound() {
        return Result.failure(new StandardApplicationError(
                "CLI_CLIENTE_NO_ENCONTRADO", "El cliente indicado no existe.", ErrorCategory.NOT_FOUND));
    }

    private static Result<ClienteResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
