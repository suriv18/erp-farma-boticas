package com.softprimesolutions.ventas.infrastructure.configuration;

import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import com.softprimesolutions.ventas.application.usecase.command.AbrirTurnoHandler;
import com.softprimesolutions.ventas.application.usecase.command.AnularVentaHandler;
import com.softprimesolutions.ventas.application.usecase.command.CerrarTurnoHandler;
import com.softprimesolutions.ventas.application.usecase.command.RegistrarVentaHandler;
import com.softprimesolutions.ventas.application.usecase.query.ConsultarTurnosHandler;
import com.softprimesolutions.ventas.application.usecase.query.ConsultarVentasHandler;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class VentasModuleConfiguration {

    @Bean
    ClockPort ventasClockPort() {
        return () -> Instant.now(Clock.systemUTC());
    }

    @Bean
    IdentifierGenerator ventasIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    ConsultarTurnosUseCase consultarTurnosUseCase(VentasReadPort readPort) {
        return new ConsultarTurnosHandler(readPort);
    }

    @Bean
    AbrirTurnoUseCase abrirTurnoUseCase(
            TurnoWritePort turnos, ReferenciasVentasPort referencias, ConsultarTurnosUseCase consultarTurnosUseCase,
            IdentifierGenerator ventasIdentifierGenerator, ClockPort ventasClockPort) {
        return new AbrirTurnoHandler(
                turnos, referencias, consultarTurnosUseCase, ventasIdentifierGenerator, ventasClockPort);
    }

    @Bean
    CerrarTurnoUseCase cerrarTurnoUseCase(
            TurnoWritePort turnos, ConsultarTurnosUseCase consultarTurnosUseCase, TransaccionPort transaccion,
            ClockPort ventasClockPort) {
        return new CerrarTurnoHandler(turnos, consultarTurnosUseCase, transaccion, ventasClockPort);
    }

    @Bean
    ConsultarVentasUseCase consultarVentasUseCase(VentasReadPort readPort) {
        return new ConsultarVentasHandler(readPort);
    }

    @Bean
    RegistrarVentaUseCase registrarVentaUseCase(
            VentaWritePort ventas, TurnoWritePort turnos, ReferenciasVentasPort referencias,
            SalidaInventarioPort inventario, NumeracionPort numeracion,
            ConsultarVentasUseCase consultarVentasUseCase, TransaccionPort transaccion,
            IdentifierGenerator ventasIdentifierGenerator, ClockPort ventasClockPort) {
        return new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultarVentasUseCase, transaccion,
                ventasIdentifierGenerator, ventasClockPort);
    }

    @Bean
    AnularVentaUseCase anularVentaUseCase(
            AnulacionWritePort anulaciones, ReintegroInventarioPort reintegro,
            ConsultarVentasUseCase consultarVentasUseCase, TransaccionPort transaccion,
            ClockPort ventasClockPort) {
        return new AnularVentaHandler(anulaciones, reintegro, consultarVentasUseCase, transaccion, ventasClockPort);
    }
}
