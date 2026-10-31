package com.softprimesolutions.clientes.application.usecase.command;

import com.softprimesolutions.clientes.application.dto.command.RegistrarClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.RegistroClienteResult;
import com.softprimesolutions.clientes.application.mapper.ClientesApplicationMapper;
import com.softprimesolutions.clientes.application.port.in.RegistrarClienteUseCase;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import com.softprimesolutions.clientes.domain.model.Cliente;
import com.softprimesolutions.clientes.domain.model.TipoCliente;
import com.softprimesolutions.clientes.domain.valueobject.ClienteId;
import com.softprimesolutions.clientes.domain.valueobject.TenantId;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class RegistrarClienteHandler implements RegistrarClienteUseCase {

    private final ClientesWritePort writePort;
    private final ClientesReadPort readPort;
    private final IdentifierGenerator identifierGenerator;
    private final ClockPort clock;

    public RegistrarClienteHandler(
            ClientesWritePort writePort, ClientesReadPort readPort,
            IdentifierGenerator identifierGenerator, ClockPort clock) {
        this.writePort = Objects.requireNonNull(writePort, "writePort es obligatorio");
        this.readPort = Objects.requireNonNull(readPort, "readPort es obligatorio");
        this.identifierGenerator = Objects.requireNonNull(identifierGenerator, "identifierGenerator es obligatorio");
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<RegistroClienteResult, ApplicationError> execute(RegistrarClienteCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        var tipoCliente = TipoCliente.parse(command.tipoCliente());
        if (tipoCliente.isEmpty()) {
            return Result.failure(new StandardApplicationError(
                    "CLI_TIPO_CLIENTE_INVALIDO", "El tipo de cliente indicado no es válido.",
                    ErrorCategory.VALIDATION, Map.of("field", "tipoCliente")));
        }
        var cliente = Cliente.create(
                new ClienteId(identifierGenerator.next()),
                command.tenantId() == null ? null : new TenantId(command.tenantId()),
                tipoCliente.get(), command.tipoDocumento(), command.numeroDocumento(), command.nombres(),
                command.apellidos(), command.razonSocial(), command.email(), command.telefono(),
                command.fechaNacimiento(), clock.now());
        return cliente.fold(this::persist, this::validationFailure);
    }

    private Result<RegistroClienteResult, ApplicationError> persist(Cliente cliente) {
        return switch (writePort.save(cliente)) {
            case CREATED -> Result.success(
                    new RegistroClienteResult(ClientesApplicationMapper.toResult(cliente), true));
            case DUPLICATE_DOCUMENTO -> existing(cliente);
            case TENANT_NOT_FOUND -> failure(
                    "CLI_TENANT_NO_ENCONTRADO", "El tenant indicado no existe.", ErrorCategory.NOT_FOUND);
            case TIPO_DOCUMENTO_INVALIDO -> failure(
                    "CLI_TIPO_DOCUMENTO_INVALIDO", "El tipo de documento indicado no existe en el catálogo.",
                    ErrorCategory.VALIDATION);
        };
    }

    private Result<RegistroClienteResult, ApplicationError> existing(Cliente cliente) {
        return readPort.findClienteByDocumento(
                        cliente.tenantId().value(), cliente.tipoDocumento(), cliente.numeroDocumento())
                .<Result<RegistroClienteResult, ApplicationError>>map(
                        existente -> Result.success(new RegistroClienteResult(existente, false)))
                .orElseGet(() -> failure(
                        "CLI_CLIENTE_DOCUMENTO_DUPLICADO", "Ya existe un cliente con el documento indicado.",
                        ErrorCategory.CONFLICT));
    }

    private static Result<RegistroClienteResult, ApplicationError> failure(
            String code, String message, ErrorCategory category) {
        return Result.failure(new StandardApplicationError(code, message, category));
    }

    private Result<RegistroClienteResult, ApplicationError> validationFailure(ErrorDetail error) {
        return Result.failure(new StandardApplicationError(
                error.code(), error.message(), ErrorCategory.VALIDATION, error.metadata()));
    }
}
