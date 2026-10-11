package com.softprimesolutions.ventas.infrastructure.configuration;

import static com.softprimesolutions.ventas.VentasFixtures.TRANSACCION_DIRECTA;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.ventas.application.port.out.ActorActualPort;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import com.softprimesolutions.ventas.application.usecase.autorizado.ControlAmbito;
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
    private final ActorActualPort actor = mock(ActorActualPort.class);
    private final ControlAmbito control = configuration.controlAmbito(mock(AmbitoPort.class));
    private final ClockPort clock = configuration.ventasClockPort();
    private final IdentifierGenerator identifiers = configuration.ventasIdentifierGenerator();

    @Test
    void providesAClockCloseToNowAndUniqueIdentifiers() {
        assertThat(Duration.between(clock.now(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
        assertThat(identifiers.next()).isNotEqualTo(identifiers.next());
    }

    @Test
    void wiresTheTurnoUseCasesWithAndWithoutScopeControl() {
        var interna = configuration.consultarTurnosInterno(readPort);

        assertThat(configuration.consultarTurnosUseCase(interna, referencias, actor, control)).isNotNull();
        var abrir = configuration.abrirTurnoInterno(turnos, referencias, interna, identifiers, clock);
        assertThat(configuration.abrirTurnoUseCase(abrir, referencias, control)).isNotNull();
        var cerrar = configuration.cerrarTurnoInterno(turnos, interna, TRANSACCION_DIRECTA, clock);
        assertThat(configuration.cerrarTurnoUseCase(cerrar, readPort, control)).isNotNull();
    }

    @Test
    void wiresTheVentaUseCasesWithAndWithoutScopeControl() {
        var interna = configuration.consultarVentasInterno(readPort);

        assertThat(configuration.consultarVentasUseCase(interna, actor, control)).isNotNull();
        var registrar = configuration.registrarVentaInterno(
                ventas, turnos, referencias, inventario, numeracion, interna, TRANSACCION_DIRECTA, identifiers,
                clock);
        assertThat(configuration.registrarVentaUseCase(registrar, referencias, control)).isNotNull();
        var anular = configuration.anularVentaInterno(anulaciones, reintegro, interna, TRANSACCION_DIRECTA, clock);
        assertThat(configuration.anularVentaUseCase(anular, readPort, control)).isNotNull();
    }
}
