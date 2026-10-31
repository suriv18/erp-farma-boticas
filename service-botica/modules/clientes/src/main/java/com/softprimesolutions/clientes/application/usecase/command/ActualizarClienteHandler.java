package com.softprimesolutions.clientes.application.usecase.command;

import com.softprimesolutions.clientes.application.dto.command.ActualizarClienteCommand;
import com.softprimesolutions.clientes.application.dto.result.ClienteResult;
import com.softprimesolutions.clientes.application.port.in.ActualizarClienteUseCase;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import com.softprimesolutions.shared.application.error.ApplicationError;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Objects;

public final class ActualizarClienteHandler implements ActualizarClienteUseCase {

    private final ModificarCliente modificar;
    private final ClockPort clock;

    public ActualizarClienteHandler(ClientesReadPort readPort, ClientesWritePort writePort, ClockPort clock) {
        this.modificar = new ModificarCliente(readPort, writePort);
        this.clock = Objects.requireNonNull(clock, "clock es obligatorio");
    }

    @Override
    public Result<ClienteResult, ApplicationError> execute(ActualizarClienteCommand command) {
        Objects.requireNonNull(command, "command es obligatorio");
        return modificar.execute(command.tenantId(), command.clienteId(), cliente -> cliente.updateDetails(
                command.nombres(), command.apellidos(), command.razonSocial(), command.email(),
                command.telefono(), command.fechaNacimiento(), clock.now()));
    }
}
