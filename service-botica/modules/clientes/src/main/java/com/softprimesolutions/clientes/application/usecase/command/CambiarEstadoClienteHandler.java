package com.softprimesolutions.clientes.application.usecase.command;

import com.softprimesolutions.clientes.application.dto.command.CambiarEstadoClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.port.in.CambiarEstadoClienteUseCase;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import com.softprimesolutions.clientes.domain.model.EstadoCliente;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.error.ErrorCategory;
import com.softprimesolutions.shared.application.error.StandardApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;
import java.util.Objects;

public final class CambiarEstadoClienteHandler implements CambiarEstadoClienteUseCase {

    private final ModificarCliente modificar;
    private final ClockPort clock;

    public CambiarEstadoClienteHandler(ClientesReadPort readPort, ClientesWritePort writePort, ClockPort clock) {
        this.modificar = new ModificarCliente(readPort, writePort);
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<ClienteResult, ApplicationError> execute(CambiarEstadoClienteCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return EstadoCliente.parse(command.estado())
                .map(estado -> modificar.execute(command.tenantId(), command.clienteId(),
                        cliente -> cliente.cambiarEstado(estado, command.motivo(), clock.now())))
                .orElseGet(() -> Result.failure(new StandardApplicationError(
                        "CLI_ESTADO_INVALIDO", "El estado indicado no es válido.", ErrorCategory.VALIDATION,
                        Map.of("field", "estado"))));
    }
}
