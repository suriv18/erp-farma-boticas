package com.softprimesolutions.catalogo.infrastructure.configuration;

import com.softprimesolutions.catalogo.application.port.in.ActualizarCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarProductoReguladoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarSkuUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ActualizarViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.in.AgregarCodigoBarraUseCase;
import com.softprimesolutions.catalogo.application.port.in.AsociarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CatalogoControlUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarProductoReguladoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ConsultarSkuUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearCategoriaProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearClasificacionControladaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearCondicionVentaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearFormaFarmaceuticaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearMarcaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearProductoReguladoUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearSkuUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearUnidadMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.in.CrearViaAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.in.DesasociarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.EliminarCodigoBarraUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarCategoriasProductoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarClasificacionesControladasUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarCondicionesVentaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarFormasFarmaceuticasUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarMarcasUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarPrincipioActivoUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarProductosReguladosUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarSkusUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarUnidadesMedidaUseCase;
import com.softprimesolutions.catalogo.application.port.in.ListarViasAdministracionUseCase;
import com.softprimesolutions.catalogo.application.port.in.MarcarCodigoBarraPrincipalUseCase;
import com.softprimesolutions.catalogo.application.port.out.CatalogoComercialPort;
import com.softprimesolutions.catalogo.application.port.out.CatalogoReadPort;
import com.softprimesolutions.catalogo.application.port.out.CatalogoSoportePort;
import com.softprimesolutions.catalogo.application.port.out.ProductoReguladoPort;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarCategoriaProductoHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarClasificacionControladaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarCondicionVentaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarFormaFarmaceuticaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarMarcaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarPrincipioActivoHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarProductoReguladoHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarSkuHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarUnidadMedidaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.ActualizarViaAdministracionHandler;
import com.softprimesolutions.catalogo.application.usecase.command.AgregarCodigoBarraHandler;
import com.softprimesolutions.catalogo.application.usecase.command.AsociarPrincipioActivoHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CatalogoControlService;
import com.softprimesolutions.catalogo.application.usecase.command.CrearCategoriaProductoHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearClasificacionControladaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearCondicionVentaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearFormaFarmaceuticaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearMarcaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearPrincipioActivoHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearProductoReguladoHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearSkuHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearUnidadMedidaHandler;
import com.softprimesolutions.catalogo.application.usecase.command.CrearViaAdministracionHandler;
import com.softprimesolutions.catalogo.application.usecase.command.DesasociarPrincipioActivoHandler;
import com.softprimesolutions.catalogo.application.usecase.command.EliminarCodigoBarraHandler;
import com.softprimesolutions.catalogo.application.usecase.command.MarcarCodigoBarraPrincipalHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarProductoReguladoHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ConsultarSkuHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarCategoriasProductoHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarClasificacionesControladasHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarCondicionesVentaHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarFormasFarmaceuticasHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarMarcasHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarPrincipioActivoHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarProductosReguladosHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarSkusHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarUnidadesMedidaHandler;
import com.softprimesolutions.catalogo.application.usecase.query.ListarViasAdministracionHandler;
import com.softprimesolutions.shared.application.port.ClockPort;
import com.softprimesolutions.shared.application.port.IdentifierGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Ensambla los casos de uso del módulo catalogo como beans Spring, cableando cada handler de
 * {@code application.usecase} con su puerto {@code out} correspondiente. Sigue el mismo patrón que
 * {@code security.infrastructure.configuration.SecurityModuleConfiguration}.
 *
 * <p>No se declara un bean {@code java.time.Clock} propio (a diferencia de
 * {@code SecurityModuleConfiguration}) porque Spring Modulith's
 * {@code EventPublicationAutoConfiguration} exige un único bean {@code Clock} sin calificar en todo
 * el contexto; declarar un segundo aquí produciría {@code NoUniqueBeanDefinitionException} al
 * ensamblar {@code bootstrap-app}. En su lugar, {@link ClockPort} usa {@code Clock.systemUTC()}
 * directamente.
 */
@Configuration(proxyBeanMethods = false)
public class CatalogoModuleConfiguration {

    @Bean
    ClockPort catalogoClockPort() {
        return () -> Instant.now(Clock.systemUTC());
    }

    @Bean
    IdentifierGenerator catalogoIdentifierGenerator() {
        return UUID::randomUUID;
    }

    // ---- Soporte: condiciones de venta ----

    @Bean
    CrearCondicionVentaUseCase crearCondicionVentaUseCase(CatalogoSoportePort soportePort) {
        return new CrearCondicionVentaHandler(soportePort);
    }

    @Bean
    ActualizarCondicionVentaUseCase actualizarCondicionVentaUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarCondicionVentaHandler(soportePort);
    }

    @Bean
    ListarCondicionesVentaUseCase listarCondicionesVentaUseCase(CatalogoReadPort readPort) {
        return new ListarCondicionesVentaHandler(readPort);
    }

    // ---- Soporte: formas farmacéuticas ----

    @Bean
    CrearFormaFarmaceuticaUseCase crearFormaFarmaceuticaUseCase(CatalogoSoportePort soportePort) {
        return new CrearFormaFarmaceuticaHandler(soportePort);
    }

    @Bean
    ActualizarFormaFarmaceuticaUseCase actualizarFormaFarmaceuticaUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarFormaFarmaceuticaHandler(soportePort);
    }

    @Bean
    ListarFormasFarmaceuticasUseCase listarFormasFarmaceuticasUseCase(CatalogoReadPort readPort) {
        return new ListarFormasFarmaceuticasHandler(readPort);
    }

    // ---- Soporte: vías de administración ----

    @Bean
    CrearViaAdministracionUseCase crearViaAdministracionUseCase(CatalogoSoportePort soportePort) {
        return new CrearViaAdministracionHandler(soportePort);
    }

    @Bean
    ActualizarViaAdministracionUseCase actualizarViaAdministracionUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarViaAdministracionHandler(soportePort);
    }

    @Bean
    ListarViasAdministracionUseCase listarViasAdministracionUseCase(CatalogoReadPort readPort) {
        return new ListarViasAdministracionHandler(readPort);
    }

    // ---- Soporte: unidades de medida ----

    @Bean
    CrearUnidadMedidaUseCase crearUnidadMedidaUseCase(CatalogoSoportePort soportePort) {
        return new CrearUnidadMedidaHandler(soportePort);
    }

    @Bean
    ActualizarUnidadMedidaUseCase actualizarUnidadMedidaUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarUnidadMedidaHandler(soportePort);
    }

    @Bean
    ListarUnidadesMedidaUseCase listarUnidadesMedidaUseCase(CatalogoReadPort readPort) {
        return new ListarUnidadesMedidaHandler(readPort);
    }

    // ---- Soporte: clasificaciones controladas ----

    @Bean
    CrearClasificacionControladaUseCase crearClasificacionControladaUseCase(CatalogoSoportePort soportePort) {
        return new CrearClasificacionControladaHandler(soportePort);
    }

    @Bean
    ActualizarClasificacionControladaUseCase actualizarClasificacionControladaUseCase(
            CatalogoSoportePort soportePort) {
        return new ActualizarClasificacionControladaHandler(soportePort);
    }

    @Bean
    ListarClasificacionesControladasUseCase listarClasificacionesControladasUseCase(CatalogoReadPort readPort) {
        return new ListarClasificacionesControladasHandler(readPort);
    }

    // ---- Principios activos ----

    @Bean
    CrearPrincipioActivoUseCase crearPrincipioActivoUseCase(
            CatalogoSoportePort soportePort, IdentifierGenerator catalogoIdentifierGenerator) {
        return new CrearPrincipioActivoHandler(soportePort, catalogoIdentifierGenerator);
    }

    @Bean
    ActualizarPrincipioActivoUseCase actualizarPrincipioActivoUseCase(CatalogoSoportePort soportePort) {
        return new ActualizarPrincipioActivoHandler(soportePort);
    }

    @Bean
    ListarPrincipioActivoUseCase listarPrincipioActivoUseCase(CatalogoReadPort readPort) {
        return new ListarPrincipioActivoHandler(readPort);
    }

    // ---- Marcas ----

    @Bean
    CrearMarcaUseCase crearMarcaUseCase(
            CatalogoComercialPort comercialPort, IdentifierGenerator catalogoIdentifierGenerator) {
        return new CrearMarcaHandler(comercialPort, catalogoIdentifierGenerator);
    }

    @Bean
    ActualizarMarcaUseCase actualizarMarcaUseCase(CatalogoComercialPort comercialPort) {
        return new ActualizarMarcaHandler(comercialPort);
    }

    @Bean
    ListarMarcasUseCase listarMarcasUseCase(CatalogoReadPort readPort) {
        return new ListarMarcasHandler(readPort);
    }

    // ---- Categorías de producto ----

    @Bean
    CrearCategoriaProductoUseCase crearCategoriaProductoUseCase(
            CatalogoComercialPort comercialPort, IdentifierGenerator catalogoIdentifierGenerator) {
        return new CrearCategoriaProductoHandler(comercialPort, catalogoIdentifierGenerator);
    }

    @Bean
    ActualizarCategoriaProductoUseCase actualizarCategoriaProductoUseCase(CatalogoComercialPort comercialPort) {
        return new ActualizarCategoriaProductoHandler(comercialPort);
    }

    @Bean
    ListarCategoriasProductoUseCase listarCategoriasProductoUseCase(CatalogoReadPort readPort) {
        return new ListarCategoriasProductoHandler(readPort);
    }

    // ---- Productos regulados ----

    @Bean
    CrearProductoReguladoUseCase crearProductoReguladoUseCase(
            ProductoReguladoPort productoReguladoPort, IdentifierGenerator catalogoIdentifierGenerator,
            ClockPort catalogoClockPort) {
        return new CrearProductoReguladoHandler(productoReguladoPort, catalogoIdentifierGenerator, catalogoClockPort);
    }

    @Bean
    ActualizarProductoReguladoUseCase actualizarProductoReguladoUseCase(
            ProductoReguladoPort productoReguladoPort, ClockPort catalogoClockPort) {
        return new ActualizarProductoReguladoHandler(productoReguladoPort, catalogoClockPort);
    }

    @Bean
    ConsultarProductoReguladoUseCase consultarProductoReguladoUseCase(ProductoReguladoPort productoReguladoPort) {
        return new ConsultarProductoReguladoHandler(productoReguladoPort);
    }

    @Bean
    ListarProductosReguladosUseCase listarProductosReguladosUseCase(CatalogoReadPort readPort) {
        return new ListarProductosReguladosHandler(readPort);
    }

    @Bean
    AsociarPrincipioActivoUseCase asociarPrincipioActivoUseCase(ProductoReguladoPort productoReguladoPort) {
        return new AsociarPrincipioActivoHandler(productoReguladoPort);
    }

    @Bean
    DesasociarPrincipioActivoUseCase desasociarPrincipioActivoUseCase(ProductoReguladoPort productoReguladoPort) {
        return new DesasociarPrincipioActivoHandler(productoReguladoPort);
    }

    // ---- SKUs ----

    @Bean
    CrearSkuUseCase crearSkuUseCase(
            CatalogoComercialPort comercialPort, IdentifierGenerator catalogoIdentifierGenerator,
            ClockPort catalogoClockPort) {
        return new CrearSkuHandler(comercialPort, catalogoIdentifierGenerator, catalogoClockPort);
    }

    @Bean
    ActualizarSkuUseCase actualizarSkuUseCase(CatalogoComercialPort comercialPort, ClockPort catalogoClockPort) {
        return new ActualizarSkuHandler(comercialPort, catalogoClockPort);
    }

    @Bean
    ConsultarSkuUseCase consultarSkuUseCase(CatalogoComercialPort comercialPort) {
        return new ConsultarSkuHandler(comercialPort);
    }

    @Bean
    ListarSkusUseCase listarSkusUseCase(CatalogoReadPort readPort) {
        return new ListarSkusHandler(readPort);
    }

    @Bean
    AgregarCodigoBarraUseCase agregarCodigoBarraUseCase(CatalogoComercialPort comercialPort) {
        return new AgregarCodigoBarraHandler(comercialPort);
    }

    @Bean
    EliminarCodigoBarraUseCase eliminarCodigoBarraUseCase(CatalogoComercialPort comercialPort) {
        return new EliminarCodigoBarraHandler(comercialPort);
    }

    @Bean
    MarcarCodigoBarraPrincipalUseCase marcarCodigoBarraPrincipalUseCase(CatalogoComercialPort comercialPort) {
        return new MarcarCodigoBarraPrincipalHandler(comercialPort);
    }

    // ---- Control de estado transversal ----

    @Bean
    CatalogoControlUseCase catalogoControlUseCase(
            CatalogoSoportePort soportePort, CatalogoComercialPort comercialPort,
            ProductoReguladoPort productoReguladoPort, ClockPort catalogoClockPort) {
        return new CatalogoControlService(soportePort, comercialPort, productoReguladoPort, catalogoClockPort);
    }
}
