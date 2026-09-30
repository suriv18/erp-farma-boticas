package com.softprimesolutions.organizacion.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class OrganizacionModuleConfigurationTest {

    private final OrganizacionModuleConfiguration configuration = new OrganizacionModuleConfiguration();
    private final OrganizacionReadPort readPort = mock(OrganizacionReadPort.class);
    private final OrganizacionWritePort writePort = mock(OrganizacionWritePort.class);
    private final ClockPort clock = configuration.organizacionClockPort();
    private final IdentifierGenerator identifiers = configuration.organizacionIdentifierGenerator();

    @Test
    void providesAClockCloseToNowAndUniqueIdentifiers() {
        assertThat(Duration.between(clock.now(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
        assertThat(identifiers.next()).isNotEqualTo(identifiers.next());
    }

    @Test
    void wiresEveryCommandUseCase() {
        assertThat(configuration.crearEmpresaOperadoraUseCase(writePort, identifiers, clock)).isNotNull();
        assertThat(configuration.actualizarEmpresaOperadoraUseCase(readPort, writePort, clock)).isNotNull();
        assertThat(configuration.crearEstablecimientoUseCase(writePort, identifiers, clock)).isNotNull();
        assertThat(configuration.actualizarEstablecimientoUseCase(readPort, writePort, clock)).isNotNull();
        assertThat(configuration.crearAlmacenUseCase(writePort, identifiers, clock)).isNotNull();
        assertThat(configuration.actualizarAlmacenUseCase(readPort, writePort, clock)).isNotNull();
        assertThat(configuration.crearTerminalPosUseCase(writePort, identifiers, clock)).isNotNull();
        assertThat(configuration.actualizarTerminalPosUseCase(readPort, writePort, clock)).isNotNull();
    }

    @Test
    void wiresEveryQueryUseCase() {
        assertThat(configuration.listarEmpresasUseCase(readPort)).isNotNull();
        assertThat(configuration.obtenerEmpresaUseCase(readPort)).isNotNull();
        assertThat(configuration.listarEstablecimientosUseCase(readPort)).isNotNull();
        assertThat(configuration.obtenerEstablecimientoUseCase(readPort)).isNotNull();
        assertThat(configuration.listarAlmacenesUseCase(readPort)).isNotNull();
        assertThat(configuration.obtenerAlmacenUseCase(readPort)).isNotNull();
        assertThat(configuration.listarTerminalesUseCase(readPort)).isNotNull();
        assertThat(configuration.obtenerTerminalUseCase(readPort)).isNotNull();
        assertThat(configuration.obtenerEstructuraCorporativaUseCase(readPort)).isNotNull();
    }
}
