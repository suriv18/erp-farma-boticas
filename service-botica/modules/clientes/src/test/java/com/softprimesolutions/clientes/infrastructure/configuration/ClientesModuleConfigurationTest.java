package com.softprimesolutions.clientes.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.softprimesolutions.clientes.application.port.out.ClientesReadPort;
import com.softprimesolutions.clientes.application.port.out.ClientesWritePort;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ClientesModuleConfigurationTest {

    private final ClientesModuleConfiguration configuration = new ClientesModuleConfiguration();
    private final ClientesReadPort readPort = mock(ClientesReadPort.class);
    private final ClientesWritePort writePort = mock(ClientesWritePort.class);

    @Test
    void providesTheCurrentInstantAndFreshIdentifiers() {
        var clock = configuration.clientesClockPort();
        var generator = configuration.clientesIdentifierGenerator();

        assertThat(Duration.between(clock.now(), Instant.now())).isLessThan(Duration.ofSeconds(5));
        assertThat(generator.next()).isNotEqualTo(generator.next());
    }

    @Test
    void assemblesTheUseCasesFromTheirPorts() {
        assertThat(configuration.registrarClienteUseCase(
                writePort, readPort, configuration.clientesIdentifierGenerator(), configuration.clientesClockPort()))
                .isNotNull();
        assertThat(configuration.actualizarClienteUseCase(readPort, writePort, configuration.clientesClockPort()))
                .isNotNull();
        assertThat(configuration.cambiarEstadoClienteUseCase(readPort, writePort, configuration.clientesClockPort()))
                .isNotNull();
        assertThat(configuration.obtenerClienteUseCase(readPort)).isNotNull();
        assertThat(configuration.listarClientesUseCase(readPort)).isNotNull();
    }
}
