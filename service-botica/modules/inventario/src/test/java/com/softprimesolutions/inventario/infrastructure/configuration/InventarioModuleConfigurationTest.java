package com.softprimesolutions.inventario.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.softprimesolutions.inventario.application.port.out.InventarioReadPort;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.ReferenciasInventarioPort;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class InventarioModuleConfigurationTest {

    private final InventarioModuleConfiguration configuration = new InventarioModuleConfiguration();
    private final InventarioReadPort readPort = mock(InventarioReadPort.class);
    private final InventarioWritePort writePort = mock(InventarioWritePort.class);
    private final ReferenciasInventarioPort referencias = mock(ReferenciasInventarioPort.class);
    private final ClockPort clock = configuration.inventarioClockPort();
    private final IdentifierGenerator identifiers = configuration.inventarioIdentifierGenerator();

    @Test
    void providesAClockCloseToNowAndUniqueIdentifiers() {
        assertThat(Duration.between(clock.now(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
        assertThat(identifiers.next()).isNotEqualTo(identifiers.next());
    }

    @Test
    void wiresEveryCommandUseCase() {
        var cambioEstado = configuration.cambioEstadoLote(writePort, clock);

        assertThat(cambioEstado).isNotNull();
        assertThat(configuration.bloquearLoteUseCase(cambioEstado)).isNotNull();
        assertThat(configuration.desbloquearLoteUseCase(cambioEstado)).isNotNull();
        var registrarMovimiento = configuration.registrarMovimientoUseCase(writePort, referencias, identifiers, clock);
        assertThat(registrarMovimiento).isNotNull();
        assertThat(configuration.ingresoInventarioApi(registrarMovimiento)).isNotNull();
    }

    @Test
    void wiresEveryQueryUseCase() {
        assertThat(configuration.listarPosicionesUseCase(readPort)).isNotNull();
        assertThat(configuration.obtenerLoteUseCase(readPort)).isNotNull();
    }
}
