package com.softprimesolutions.clientes.infrastructure.configuration;

import com.softprimesolutions.clientes.application.port.in.ActualizarClienteUseCase;
import com.softprimesolutions.clientes.application.port.in.CambiarEstadoClienteUseCase;
import com.softprimesolutions.clientes.application.port.in.ListarClientesUseCase;
import com.softprimesolutions.clientes.application.port.in.ObtenerClienteUseCase;
import com.softprimesolutions.clientes.application.port.in.RegistrarClienteUseCase;
import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import com.softprimesolutions.clientes.application.usecase.command.ActualizarClienteHandler;
import com.softprimesolutions.clientes.application.usecase.command.CambiarEstadoClienteHandler;
import com.softprimesolutions.clientes.application.usecase.command.RegistrarClienteHandler;
import com.softprimesolutions.clientes.application.usecase.query.ListarClientesHandler;
import com.softprimesolutions.clientes.application.usecase.query.ObtenerClienteHandler;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla los casos de uso del módulo clientes como beans Spring. No declara un bean
 * {@code java.time.Clock}: Spring Modulith exige uno solo sin calificar en todo el contexto.
 */
@Configuration(proxyBeanMethods = false)
public class ClientesModuleConfiguration {

    @Bean
    ClockPort clientesClockPort() {
        return () -> Instant.now(Clock.systemUTC());
    }

    @Bean
    IdentifierGenerator clientesIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    RegistrarClienteUseCase registrarClienteUseCase(
            ClientesWritePort writePort, ClientesReadPort readPort,
            IdentifierGenerator clientesIdentifierGenerator, ClockPort clientesClockPort) {
        return new RegistrarClienteHandler(writePort, readPort, clientesIdentifierGenerator, clientesClockPort);
    }

    @Bean
    ActualizarClienteUseCase actualizarClienteUseCase(
            ClientesReadPort readPort, ClientesWritePort writePort, ClockPort clientesClockPort) {
        return new ActualizarClienteHandler(readPort, writePort, clientesClockPort);
    }

    @Bean
    CambiarEstadoClienteUseCase cambiarEstadoClienteUseCase(
            ClientesReadPort readPort, ClientesWritePort writePort, ClockPort clientesClockPort) {
        return new CambiarEstadoClienteHandler(readPort, writePort, clientesClockPort);
    }

    @Bean
    ObtenerClienteUseCase obtenerClienteUseCase(ClientesReadPort readPort) {
        return new ObtenerClienteHandler(readPort);
    }

    @Bean
    ListarClientesUseCase listarClientesUseCase(ClientesReadPort readPort) {
        return new ListarClientesHandler(readPort);
    }
}
