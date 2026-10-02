package com.softprimesolutions.inventario.infrastructure.configuration;

import com.softprimesolutions.inventario.api.IngresoInventarioApi;
import com.softprimesolutions.inventario.api.facade.IngresoInventarioFacade;
import com.softprimesolutions.inventario.application.port.in.BloquearLoteUseCase;
import com.softprimesolutions.inventario.application.port.in.DesbloquearLoteUseCase;
import com.softprimesolutions.inventario.application.port.in.ListarPosicionesUseCase;
import com.softprimesolutions.inventario.application.port.in.ObtenerLoteUseCase;
import com.softprimesolutions.inventario.application.port.in.RegistrarMovimientoUseCase;
import com.softprimesolutions.inventario.application.port.out.InventarioReadPort;
import com.softprimesolutions.inventario.application.port.out.InventarioWritePort;
import com.softprimesolutions.inventario.application.port.out.ReferenciasInventarioPort;
import com.softprimesolutions.inventario.application.usecase.command.BloquearLoteHandler;
import com.softprimesolutions.inventario.application.usecase.command.CambioEstadoLote;
import com.softprimesolutions.inventario.application.usecase.command.DesbloquearLoteHandler;
import com.softprimesolutions.inventario.application.usecase.command.RegistrarMovimientoHandler;
import com.softprimesolutions.inventario.application.usecase.query.ListarPosicionesHandler;
import com.softprimesolutions.inventario.application.usecase.query.ObtenerLoteHandler;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class InventarioModuleConfiguration {

    @Bean
    ClockPort inventarioClockPort() {
        return () -> Instant.now(Clock.systemUTC());
    }

    @Bean
    IdentifierGenerator inventarioIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    CambioEstadoLote cambioEstadoLote(InventarioWritePort writePort, ClockPort inventarioClockPort) {
        return new CambioEstadoLote(writePort, inventarioClockPort);
    }

    @Bean
    BloquearLoteUseCase bloquearLoteUseCase(CambioEstadoLote cambioEstadoLote) {
        return new BloquearLoteHandler(cambioEstadoLote);
    }

    @Bean
    DesbloquearLoteUseCase desbloquearLoteUseCase(CambioEstadoLote cambioEstadoLote) {
        return new DesbloquearLoteHandler(cambioEstadoLote);
    }

    @Bean
    RegistrarMovimientoUseCase registrarMovimientoUseCase(
            InventarioWritePort writePort, ReferenciasInventarioPort referencias,
            IdentifierGenerator inventarioIdentifierGenerator, ClockPort inventarioClockPort) {
        return new RegistrarMovimientoHandler(
                writePort, referencias, inventarioIdentifierGenerator, inventarioClockPort);
    }

    @Bean
    IngresoInventarioApi ingresoInventarioApi(RegistrarMovimientoUseCase registrarMovimientoUseCase) {
        return new IngresoInventarioFacade(registrarMovimientoUseCase);
    }

    @Bean
    ListarPosicionesUseCase listarPosicionesUseCase(InventarioReadPort readPort) {
        return new ListarPosicionesHandler(readPort);
    }

    @Bean
    ObtenerLoteUseCase obtenerLoteUseCase(InventarioReadPort readPort) {
        return new ObtenerLoteHandler(readPort);
    }
}
