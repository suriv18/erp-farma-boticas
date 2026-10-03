package com.softprimesolutions.ventas.infrastructure.configuration;

import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class VentasModuleConfigurationTest {

    private final VentasModuleConfiguration configuration = new VentasModuleConfiguration();
    private final TurnoWritePort turnos = mock(TurnoWritePort.class);
    private final VentaWritePort ventas = mock(VentaWritePort.class);
    private final ReferenciasVentasPort referencias = mock(ReferenciasVentasPort.class);
    private final VentasReadPort readPort = mock(VentasReadPort.class);
    private final SalidaInventarioPort inventario = mock(SalidaInventarioPort.class);
    private final NumeracionPort numeracion = mock(NumeracionPort.class);
    private final AnulacionWritePort anulaciones = mock(AnulacionWritePort.class);
    private final ReintegroInventarioPort reintegro = mock(ReintegroInventarioPort.class);
    private final ClockPort clock = configuration.ventasClockPort();
    private final IdentifierGenerator identifiers = configuration.ventasIdentifierGenerator();

    @Test
    void providesAClockCloseToNowAndUniqueIdentifiers() {
        assertThat(Duration.between(clock.now(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
        assertThat(identifiers.next()).isNotEqualTo(identifiers.next());
    }

    @Test
    void wiresTheTurnoUseCases() {
        var consultas = configuration.consultarTurnosUseCase(readPort);

        assertThat(consultas).isNotNull();
        assertThat(configuration.abrirTurnoUseCase(turnos, referencias, consultas, identifiers, clock)).isNotNull();
        assertThat(configuration.cerrarTurnoUseCase(turnos, consultas, TRANSACCION_DIRECTA, clock)).isNotNull();
    }

    @Test
    void wiresTheVentaUseCases() {
        var consultas = configuration.consultarVentasUseCase(readPort);

        assertThat(consultas).isNotNull();
        assertThat(configuration.registrarVentaUseCase(
                ventas, turnos, referencias, inventario, numeracion, consultas, TRANSACCION_DIRECTA, identifiers,
                clock)).isNotNull();
    }

    @Test
    void wiresTheAnularVentaUseCase() {
        var consultas = configuration.consultarVentasUseCase(readPort);

        assertThat(configuration.anularVentaUseCase(anulaciones, reintegro, consultas, TRANSACCION_DIRECTA, clock))
                .isNotNull();
    }
}
