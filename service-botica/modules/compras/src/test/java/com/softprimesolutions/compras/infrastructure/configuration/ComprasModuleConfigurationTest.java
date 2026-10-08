package com.softprimesolutions.compras.infrastructure.configuration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort;
import com.softprimesolutions.compras.application.port.out.NumeracionPort;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.application.port.out.RecepcionWritePort;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort;
import com.softprimesolutions.compras.application.port.out.TransaccionPort;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

class ComprasModuleConfigurationTest {

    private final ComprasModuleConfiguration configuration = new ComprasModuleConfiguration();
    private final ProveedorWritePort proveedores = mock(ProveedorWritePort.class);
    private final OrdenCompraWritePort ordenes = mock(OrdenCompraWritePort.class);
    private final RecepcionWritePort recepciones = mock(RecepcionWritePort.class);
    private final ComprasReadPort readPort = mock(ComprasReadPort.class);
    private final ReferenciasComprasPort referencias = mock(ReferenciasComprasPort.class);
    private final IngresoInventarioPort inventario = mock(IngresoInventarioPort.class);
    private final NumeracionPort numeracion = mock(NumeracionPort.class);
    private final TransaccionPort transaccion = mock(TransaccionPort.class);
    private final ClockPort clock = configuration.comprasClockPort();
    private final IdentifierGenerator identifiers = configuration.comprasIdentifierGenerator();

    @Test
    void providesAClockCloseToNowAndUniqueIdentifiers() {
        assertThat(Duration.between(clock.now(), Instant.now()).abs()).isLessThan(Duration.ofSeconds(5));
        assertThat(identifiers.next()).isNotEqualTo(identifiers.next());
    }

    @Test
    void wiresEveryCommandUseCase() {
        assertThat(configuration.crearProveedorUseCase(proveedores, identifiers, clock)).isNotNull();
        assertThat(configuration.actualizarProveedorUseCase(proveedores, clock)).isNotNull();
        assertThat(configuration.cambiarEstadoProveedorUseCase(proveedores, clock)).isNotNull();
        assertThat(configuration.crearOrdenCompraUseCase(ordenes, proveedores, referencias, numeracion, identifiers, clock))
                .isNotNull();
        assertThat(configuration.transicionarOrdenCompraUseCase(ordenes, clock)).isNotNull();
        assertThat(configuration.registrarRecepcionUseCase(
                ordenes, recepciones, readPort, referencias, inventario, numeracion, transaccion, identifiers, clock))
                .isNotNull();
    }

    @Test
    void wiresEveryQueryUseCase() {
        assertThat(configuration.consultarProveedoresUseCase(readPort)).isNotNull();
        assertThat(configuration.consultarOrdenesCompraUseCase(readPort)).isNotNull();
        assertThat(configuration.consultarRecepcionesUseCase(readPort)).isNotNull();
    }
}
