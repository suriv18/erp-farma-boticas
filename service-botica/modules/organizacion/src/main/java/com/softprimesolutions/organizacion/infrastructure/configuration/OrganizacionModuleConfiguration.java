package com.softprimesolutions.organizacion.infrastructure.configuration;

import com.softprimesolutions.organizacion.application.port.in.ActualizarAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.ActualizarEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.ActualizarTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.in.CambiarEstadoEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEmpresaOperadoraUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.CrearTerminalPosUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarAlmacenesUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEmpresasUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarEstablecimientosUseCase;
import com.softprimesolutions.organizacion.application.port.in.ListarTerminalesUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerAlmacenUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEmpresaUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstablecimientoUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerEstructuraCorporativaUseCase;
import com.softprimesolutions.organizacion.application.port.in.ObtenerTerminalUseCase;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionReadPort;
import com.softprimesolutions.organizacion.application.port.out.OrganizacionWritePort;
import com.softprimesolutions.organizacion.application.usecase.command.ActualizarAlmacenHandler;
import com.softprimesolutions.organizacion.application.usecase.command.ActualizarEmpresaOperadoraHandler;
import com.softprimesolutions.organizacion.application.usecase.command.ActualizarEstablecimientoHandler;
import com.softprimesolutions.organizacion.application.usecase.command.ActualizarTerminalPosHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CambiarEstadoEmpresaHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CambiarEstadoEstablecimientoHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CrearAlmacenHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CrearEmpresaOperadoraHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CrearEstablecimientoHandler;
import com.softprimesolutions.organizacion.application.usecase.command.CrearTerminalPosHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ListarAlmacenesHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ListarEmpresasHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ListarEstablecimientosHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ListarTerminalesHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerAlmacenHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerEmpresaHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerEstablecimientoHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerEstructuraCorporativaHandler;
import com.softprimesolutions.organizacion.application.usecase.query.ObtenerTerminalHandler;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla los casos de uso del módulo organizacion como beans Spring. No declara un bean
 * {@code java.time.Clock}: Spring Modulith exige uno solo sin calificar en todo el contexto, por lo
 * que {@link ClockPort} usa {@code Clock.systemUTC()} directamente, igual que catalogo.
 */
@Configuration(proxyBeanMethods = false)
public class OrganizacionModuleConfiguration {

    @Bean
    ClockPort organizacionClockPort() {
        return () -> Instant.now(Clock.systemUTC());
    }

    @Bean
    IdentifierGenerator organizacionIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    CrearEmpresaOperadoraUseCase crearEmpresaOperadoraUseCase(
            OrganizacionWritePort writePort, IdentifierGenerator organizacionIdentifierGenerator,
            ClockPort organizacionClockPort) {
        return new CrearEmpresaOperadoraHandler(writePort, organizacionIdentifierGenerator, organizacionClockPort);
    }

    @Bean
    ActualizarEmpresaOperadoraUseCase actualizarEmpresaOperadoraUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new ActualizarEmpresaOperadoraHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    CambiarEstadoEmpresaUseCase cambiarEstadoEmpresaUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new CambiarEstadoEmpresaHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    CambiarEstadoEstablecimientoUseCase cambiarEstadoEstablecimientoUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new CambiarEstadoEstablecimientoHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    ListarEmpresasUseCase listarEmpresasUseCase(OrganizacionReadPort readPort) {
        return new ListarEmpresasHandler(readPort);
    }

    @Bean
    ObtenerEmpresaUseCase obtenerEmpresaUseCase(OrganizacionReadPort readPort) {
        return new ObtenerEmpresaHandler(readPort);
    }

    @Bean
    CrearEstablecimientoUseCase crearEstablecimientoUseCase(
            OrganizacionWritePort writePort, IdentifierGenerator organizacionIdentifierGenerator,
            ClockPort organizacionClockPort) {
        return new CrearEstablecimientoHandler(writePort, organizacionIdentifierGenerator, organizacionClockPort);
    }

    @Bean
    ActualizarEstablecimientoUseCase actualizarEstablecimientoUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new ActualizarEstablecimientoHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    ListarEstablecimientosUseCase listarEstablecimientosUseCase(OrganizacionReadPort readPort) {
        return new ListarEstablecimientosHandler(readPort);
    }

    @Bean
    ObtenerEstablecimientoUseCase obtenerEstablecimientoUseCase(OrganizacionReadPort readPort) {
        return new ObtenerEstablecimientoHandler(readPort);
    }

    @Bean
    CrearAlmacenUseCase crearAlmacenUseCase(
            OrganizacionWritePort writePort, IdentifierGenerator organizacionIdentifierGenerator,
            ClockPort organizacionClockPort) {
        return new CrearAlmacenHandler(writePort, organizacionIdentifierGenerator, organizacionClockPort);
    }

    @Bean
    ActualizarAlmacenUseCase actualizarAlmacenUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new ActualizarAlmacenHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    ListarAlmacenesUseCase listarAlmacenesUseCase(OrganizacionReadPort readPort) {
        return new ListarAlmacenesHandler(readPort);
    }

    @Bean
    ObtenerAlmacenUseCase obtenerAlmacenUseCase(OrganizacionReadPort readPort) {
        return new ObtenerAlmacenHandler(readPort);
    }

    @Bean
    CrearTerminalPosUseCase crearTerminalPosUseCase(
            OrganizacionWritePort writePort, IdentifierGenerator organizacionIdentifierGenerator,
            ClockPort organizacionClockPort) {
        return new CrearTerminalPosHandler(writePort, organizacionIdentifierGenerator, organizacionClockPort);
    }

    @Bean
    ActualizarTerminalPosUseCase actualizarTerminalPosUseCase(
            OrganizacionReadPort readPort, OrganizacionWritePort writePort, ClockPort organizacionClockPort) {
        return new ActualizarTerminalPosHandler(readPort, writePort, organizacionClockPort);
    }

    @Bean
    ListarTerminalesUseCase listarTerminalesUseCase(OrganizacionReadPort readPort) {
        return new ListarTerminalesHandler(readPort);
    }

    @Bean
    ObtenerTerminalUseCase obtenerTerminalUseCase(OrganizacionReadPort readPort) {
        return new ObtenerTerminalHandler(readPort);
    }

    @Bean
    ObtenerEstructuraCorporativaUseCase obtenerEstructuraCorporativaUseCase(OrganizacionReadPort readPort) {
        return new ObtenerEstructuraCorporativaHandler(readPort);
    }
}
