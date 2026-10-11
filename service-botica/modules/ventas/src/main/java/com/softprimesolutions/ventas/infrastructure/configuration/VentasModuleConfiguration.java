package com.softprimesolutions.ventas.infrastructure.configuration;

import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import com.softprimesolutions.ventas.application.port.in.AbrirTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.AnularVentaUseCase;
import com.softprimesolutions.ventas.application.port.in.CerrarTurnoUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarTurnosUseCase;
import com.softprimesolutions.ventas.application.port.in.ConsultarVentasUseCase;
import com.softprimesolutions.ventas.application.port.in.RegistrarVentaUseCase;
import com.softprimesolutions.ventas.application.port.out.ActorActualPort;
import com.softprimesolutions.ventas.application.port.out.AmbitoPort;
import com.softprimesolutions.ventas.application.port.out.AnulacionWritePort;
import com.softprimesolutions.ventas.application.port.out.NumeracionPort;
import com.softprimesolutions.ventas.application.port.out.ReferenciasVentasPort;
import com.softprimesolutions.ventas.application.port.out.ReintegroInventarioPort;
import com.softprimesolutions.ventas.application.port.out.SalidaInventarioPort;
import com.softprimesolutions.ventas.application.port.out.TransaccionPort;
import com.softprimesolutions.ventas.application.port.out.TurnoWritePort;
import com.softprimesolutions.ventas.application.port.out.VentaWritePort;
import com.softprimesolutions.ventas.application.port.out.VentasReadPort;
import com.softprimesolutions.ventas.application.usecase.autorizado.AbrirTurnoAutorizado;
import com.softprimesolutions.ventas.application.usecase.autorizado.AnularVentaAutorizado;
import com.softprimesolutions.ventas.application.usecase.autorizado.CerrarTurnoAutorizado;
import com.softprimesolutions.ventas.application.usecase.autorizado.ConsultarTurnosAutorizado;
import com.softprimesolutions.ventas.application.usecase.autorizado.ConsultarVentasAutorizado;
import com.softprimesolutions.ventas.application.usecase.autorizado.ControlAmbito;
import com.softprimesolutions.ventas.application.usecase.autorizado.RegistrarVentaAutorizado;
import com.softprimesolutions.ventas.application.usecase.command.AbrirTurnoHandler;
import com.softprimesolutions.ventas.application.usecase.command.AnularVentaHandler;
import com.softprimesolutions.ventas.application.usecase.command.CerrarTurnoHandler;
import com.softprimesolutions.ventas.application.usecase.command.RegistrarVentaHandler;
import com.softprimesolutions.ventas.application.usecase.query.ConsultarTurnosHandler;
import com.softprimesolutions.ventas.application.usecase.query.ConsultarVentasHandler;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration(proxyBeanMethods = false)
public class VentasModuleConfiguration {

    private static final String CONSULTAR_TURNOS_INTERNO = "consultarTurnosInterno";
    private static final String CONSULTAR_VENTAS_INTERNO = "consultarVentasInterno";
    private static final String ABRIR_TURNO_INTERNO = "abrirTurnoInterno";
    private static final String CERRAR_TURNO_INTERNO = "cerrarTurnoInterno";
    private static final String REGISTRAR_VENTA_INTERNO = "registrarVentaInterno";
    private static final String ANULAR_VENTA_INTERNO = "anularVentaInterno";

    @Bean
    ClockPort ventasClockPort() {
        return () -> Instant.now(Clock.systemUTC());
    }

    @Bean
    IdentifierGenerator ventasIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    ControlAmbito controlAmbito(AmbitoPort ambito) {
        return new ControlAmbito(ambito);
    }

    @Bean(CONSULTAR_TURNOS_INTERNO)
    ConsultarTurnosUseCase consultarTurnosInterno(VentasReadPort readPort) {
        return new ConsultarTurnosHandler(readPort);
    }

    @Bean
    @Primary
    ConsultarTurnosUseCase consultarTurnosUseCase(
            @Qualifier(CONSULTAR_TURNOS_INTERNO) ConsultarTurnosUseCase interno, ReferenciasVentasPort referencias,
            ActorActualPort actor, ControlAmbito control) {
        return new ConsultarTurnosAutorizado(interno, referencias, actor, control);
    }

    @Bean(ABRIR_TURNO_INTERNO)
    AbrirTurnoUseCase abrirTurnoInterno(
            TurnoWritePort turnos, ReferenciasVentasPort referencias,
            @Qualifier(CONSULTAR_TURNOS_INTERNO) ConsultarTurnosUseCase consultarTurnos,
            IdentifierGenerator ventasIdentifierGenerator, ClockPort ventasClockPort) {
        return new AbrirTurnoHandler(
                turnos, referencias, consultarTurnos, ventasIdentifierGenerator, ventasClockPort);
    }

    @Bean
    @Primary
    AbrirTurnoUseCase abrirTurnoUseCase(
            @Qualifier(ABRIR_TURNO_INTERNO) AbrirTurnoUseCase interno, ReferenciasVentasPort referencias,
            ControlAmbito control) {
        return new AbrirTurnoAutorizado(interno, referencias, control);
    }

    @Bean(CERRAR_TURNO_INTERNO)
    CerrarTurnoUseCase cerrarTurnoInterno(
            TurnoWritePort turnos, @Qualifier(CONSULTAR_TURNOS_INTERNO) ConsultarTurnosUseCase consultarTurnos,
            TransaccionPort transaccion, ClockPort ventasClockPort) {
        return new CerrarTurnoHandler(turnos, consultarTurnos, transaccion, ventasClockPort);
    }

    @Bean
    @Primary
    CerrarTurnoUseCase cerrarTurnoUseCase(
            @Qualifier(CERRAR_TURNO_INTERNO) CerrarTurnoUseCase interno, VentasReadPort readPort,
            ControlAmbito control) {
        return new CerrarTurnoAutorizado(interno, readPort, control);
    }

    @Bean(CONSULTAR_VENTAS_INTERNO)
    ConsultarVentasUseCase consultarVentasInterno(VentasReadPort readPort) {
        return new ConsultarVentasHandler(readPort);
    }

    @Bean
    @Primary
    ConsultarVentasUseCase consultarVentasUseCase(
            @Qualifier(CONSULTAR_VENTAS_INTERNO) ConsultarVentasUseCase interno, ActorActualPort actor,
            ControlAmbito control) {
        return new ConsultarVentasAutorizado(interno, actor, control);
    }

    @Bean(REGISTRAR_VENTA_INTERNO)
    RegistrarVentaUseCase registrarVentaInterno(
            VentaWritePort ventas, TurnoWritePort turnos, ReferenciasVentasPort referencias,
            SalidaInventarioPort inventario, NumeracionPort numeracion,
            @Qualifier(CONSULTAR_VENTAS_INTERNO) ConsultarVentasUseCase consultarVentas,
            TransaccionPort transaccion, IdentifierGenerator ventasIdentifierGenerator, ClockPort ventasClockPort) {
        return new RegistrarVentaHandler(
                ventas, turnos, referencias, inventario, numeracion, consultarVentas, transaccion,
                ventasIdentifierGenerator, ventasClockPort);
    }

    @Bean
    @Primary
    RegistrarVentaUseCase registrarVentaUseCase(
            @Qualifier(REGISTRAR_VENTA_INTERNO) RegistrarVentaUseCase interno, ReferenciasVentasPort referencias,
            ControlAmbito control) {
        return new RegistrarVentaAutorizado(interno, referencias, control);
    }

    @Bean(ANULAR_VENTA_INTERNO)
    AnularVentaUseCase anularVentaInterno(
            AnulacionWritePort anulaciones, ReintegroInventarioPort reintegro,
            @Qualifier(CONSULTAR_VENTAS_INTERNO) ConsultarVentasUseCase consultarVentas,
            TransaccionPort transaccion, ClockPort ventasClockPort) {
        return new AnularVentaHandler(anulaciones, reintegro, consultarVentas, transaccion, ventasClockPort);
    }

    @Bean
    @Primary
    AnularVentaUseCase anularVentaUseCase(
            @Qualifier(ANULAR_VENTA_INTERNO) AnularVentaUseCase interno, VentasReadPort readPort,
            ControlAmbito control) {
        return new AnularVentaAutorizado(interno, readPort, control);
    }
}
