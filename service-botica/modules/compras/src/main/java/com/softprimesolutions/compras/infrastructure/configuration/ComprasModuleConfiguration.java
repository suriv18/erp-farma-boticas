package com.softprimesolutions.compras.infrastructure.configuration;

import com.softprimesolutions.compras.application.port.in.ActualizarProveedorUseCase;
import com.softprimesolutions.compras.application.port.in.CambiarEstadoProveedorUseCase;
import com.softprimesolutions.compras.application.port.in.ConsultarOrdenesCompraUseCase;
import com.softprimesolutions.compras.application.port.in.ConsultarProveedoresUseCase;
import com.softprimesolutions.compras.application.port.in.ConsultarRecepcionesUseCase;
import com.softprimesolutions.compras.application.port.in.CrearOrdenCompraUseCase;
import com.softprimesolutions.compras.application.port.in.CrearProveedorUseCase;
import com.softprimesolutions.compras.application.port.in.RegistrarRecepcionUseCase;
import com.softprimesolutions.compras.application.port.in.TransicionarOrdenCompraUseCase;
import com.softprimesolutions.compras.application.port.out.ComprasReadPort;
import com.softprimesolutions.compras.application.port.out.IngresoInventarioPort;
import com.softprimesolutions.compras.application.port.out.NumeracionPort;
import com.softprimesolutions.compras.application.port.out.OrdenCompraWritePort;
import com.softprimesolutions.compras.application.port.out.ProveedorWritePort;
import com.softprimesolutions.compras.application.port.out.RecepcionWritePort;
import com.softprimesolutions.compras.application.port.out.ReferenciasComprasPort;
import com.softprimesolutions.compras.application.port.out.TransaccionPort;
import com.softprimesolutions.compras.application.usecase.command.ActualizarProveedorHandler;
import com.softprimesolutions.compras.application.usecase.command.CambiarEstadoProveedorHandler;
import com.softprimesolutions.compras.application.usecase.command.CrearOrdenCompraHandler;
import com.softprimesolutions.compras.application.usecase.command.CrearProveedorHandler;
import com.softprimesolutions.compras.application.usecase.command.RegistrarRecepcionHandler;
import com.softprimesolutions.compras.application.usecase.command.TransicionarOrdenCompraHandler;
import com.softprimesolutions.compras.application.usecase.query.ConsultarOrdenesCompraHandler;
import com.softprimesolutions.compras.application.usecase.query.ConsultarProveedoresHandler;
import com.softprimesolutions.compras.application.usecase.query.ConsultarRecepcionesHandler;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ComprasModuleConfiguration {

    @Bean
    ClockPort comprasClockPort() {
        return () -> Instant.now(Clock.systemUTC());
    }

    @Bean
    IdentifierGenerator comprasIdentifierGenerator() {
        return UUID::randomUUID;
    }

    @Bean
    CrearProveedorUseCase crearProveedorUseCase(
            ProveedorWritePort proveedores, IdentifierGenerator comprasIdentifierGenerator, ClockPort comprasClockPort) {
        return new CrearProveedorHandler(proveedores, comprasIdentifierGenerator, comprasClockPort);
    }

    @Bean
    ActualizarProveedorUseCase actualizarProveedorUseCase(ProveedorWritePort proveedores, ClockPort comprasClockPort) {
        return new ActualizarProveedorHandler(proveedores, comprasClockPort);
    }

    @Bean
    CambiarEstadoProveedorUseCase cambiarEstadoProveedorUseCase(
            ProveedorWritePort proveedores, ClockPort comprasClockPort) {
        return new CambiarEstadoProveedorHandler(proveedores, comprasClockPort);
    }

    @Bean
    ConsultarProveedoresUseCase consultarProveedoresUseCase(ComprasReadPort readPort) {
        return new ConsultarProveedoresHandler(readPort);
    }

    @Bean
    CrearOrdenCompraUseCase crearOrdenCompraUseCase(
            OrdenCompraWritePort ordenes, ProveedorWritePort proveedores, ReferenciasComprasPort referencias,
            NumeracionPort numeracion, IdentifierGenerator comprasIdentifierGenerator, ClockPort comprasClockPort) {
        return new CrearOrdenCompraHandler(
                ordenes, proveedores, referencias, numeracion, comprasIdentifierGenerator, comprasClockPort);
    }

    @Bean
    TransicionarOrdenCompraUseCase transicionarOrdenCompraUseCase(
            OrdenCompraWritePort ordenes, ClockPort comprasClockPort) {
        return new TransicionarOrdenCompraHandler(ordenes, comprasClockPort);
    }

    @Bean
    ConsultarOrdenesCompraUseCase consultarOrdenesCompraUseCase(ComprasReadPort readPort) {
        return new ConsultarOrdenesCompraHandler(readPort);
    }

    @Bean
    RegistrarRecepcionUseCase registrarRecepcionUseCase(
            OrdenCompraWritePort ordenes, RecepcionWritePort recepciones, ComprasReadPort readPort,
            ReferenciasComprasPort referencias, IngresoInventarioPort inventario, NumeracionPort numeracion,
            TransaccionPort transaccion, IdentifierGenerator comprasIdentifierGenerator, ClockPort comprasClockPort) {
        return new RegistrarRecepcionHandler(
                ordenes, recepciones, readPort, referencias, inventario, numeracion, transaccion,
                comprasIdentifierGenerator, comprasClockPort);
    }

    @Bean
    ConsultarRecepcionesUseCase consultarRecepcionesUseCase(ComprasReadPort readPort) {
        return new ConsultarRecepcionesHandler(readPort);
    }
}
