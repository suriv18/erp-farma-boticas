package com.softprimesolutions.organizacion.api.mapper;

import com.softprimesolutions.organizacion.api.dto.request.ActualizarAlmacenRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.dto.request.ActualizarTerminalPosRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearAlmacenRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEmpresaOperadoraRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearEstablecimientoRequest;
import com.softprimesolutions.organizacion.api.dto.request.CrearTerminalPosRequest;
import com.softprimesolutions.organizacion.api.dto.response.AlmacenResponse;
import com.softprimesolutions.organizacion.api.dto.response.EmpresaNodoResponse;
import com.softprimesolutions.organizacion.api.dto.response.EmpresaOperadoraResponse;
import com.softprimesolutions.organizacion.api.dto.response.EstablecimientoNodoResponse;
import com.softprimesolutions.organizacion.api.dto.response.EstablecimientoResponse;
import com.softprimesolutions.organizacion.api.dto.response.EstructuraCorporativaResponse;
import com.softprimesolutions.organizacion.api.dto.response.NodoResponse;
import com.softprimesolutions.organizacion.api.dto.response.PaginaResponse;
import com.softprimesolutions.organizacion.api.dto.response.TerminalPosResponse;
import com.softprimesolutions.organizacion.application.dto.command.ActualizarAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.command.ActualizarEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.command.ActualizarEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.command.ActualizarTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.command.CrearAlmacenCommand;
import com.softprimesolutions.organizacion.application.dto.command.CrearEmpresaOperadoraCommand;
import com.softprimesolutions.organizacion.application.dto.command.CrearEstablecimientoCommand;
import com.softprimesolutions.organizacion.application.dto.command.CrearTerminalPosCommand;
import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoNodoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.EstructuraCorporativaResult;
import com.softprimesolutions.organizacion.application.dto.result.NodoResult;
import com.softprimesolutions.organizacion.application.dto.result.PaginaResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import java.util.UUID;
import java.util.function.Function;

public final class OrganizacionApiMapper {

    private OrganizacionApiMapper() {
    }

    public static CrearEmpresaOperadoraCommand toCommand(CrearEmpresaOperadoraRequest request) {
        return new CrearEmpresaOperadoraCommand(
                request.tenantId(), request.ruc(), request.razonSocial(), request.nombreComercial(),
                request.direccionFiscal(), request.ubigeoFiscal(), request.telefono(), request.email(),
                request.sitioWeb(), request.monedaFuncional(), request.zonaHoraria(),
                request.permiteVentaOnline());
    }

    public static ActualizarEmpresaOperadoraCommand toCommand(
            UUID empresaId, UUID tenantId, ActualizarEmpresaOperadoraRequest request) {
        return new ActualizarEmpresaOperadoraCommand(
                empresaId, tenantId, request.razonSocial(), request.nombreComercial(),
                request.direccionFiscal(), request.ubigeoFiscal(), request.telefono(), request.email(),
                request.sitioWeb(), request.monedaFuncional(), request.zonaHoraria(),
                request.permiteVentaOnline());
    }

    public static EmpresaOperadoraResponse toResponse(EmpresaOperadoraResult result) {
        return new EmpresaOperadoraResponse(
                result.id(), result.tenantId(), result.ruc(), result.razonSocial(), result.nombreComercial(),
                result.direccionFiscal(), result.ubigeoFiscal(), result.telefono(), result.email(),
                result.sitioWeb(), result.monedaFuncional(), result.zonaHoraria(), result.permiteVentaOnline(),
                result.estado(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<EmpresaOperadoraResponse> toEmpresaPage(PaginaResult<EmpresaOperadoraResult> result) {
        return toPage(result, OrganizacionApiMapper::toResponse);
    }

    public static CrearEstablecimientoCommand toCommand(CrearEstablecimientoRequest request) {
        return new CrearEstablecimientoCommand(
                request.tenantId(), request.empresaId(), request.codigo(), request.nombre(),
                request.tipoEstablecimiento(), request.categoriaRegulatoriaCodigo(),
                request.codigoAnexoSunat(), request.codigoDigemid(), request.direccion(), request.ubigeo(),
                request.referencia(), request.latitud(), request.longitud(), request.telefono(),
                request.email(), request.esPrincipal(), request.permiteVentaOnline(),
                request.permiteDelivery(), request.perfilOperacion(), request.zonaHoraria());
    }

    public static ActualizarEstablecimientoCommand toCommand(
            UUID establecimientoId, UUID tenantId, ActualizarEstablecimientoRequest request) {
        return new ActualizarEstablecimientoCommand(
                establecimientoId, tenantId, request.nombre(), request.tipoEstablecimiento(),
                request.categoriaRegulatoriaCodigo(), request.codigoAnexoSunat(), request.codigoDigemid(),
                request.direccion(), request.ubigeo(), request.referencia(), request.latitud(),
                request.longitud(), request.telefono(), request.email(), request.esPrincipal(),
                request.permiteVentaOnline(), request.permiteDelivery(), request.perfilOperacion(),
                request.zonaHoraria());
    }

    public static EstablecimientoResponse toResponse(EstablecimientoResult result) {
        return new EstablecimientoResponse(
                result.id(), result.tenantId(), result.empresaId(), result.codigo(), result.nombre(),
                result.tipoEstablecimiento(), result.categoriaRegulatoriaCodigo(), result.codigoAnexoSunat(),
                result.codigoDigemid(), result.direccion(), result.ubigeo(), result.referencia(),
                result.latitud(), result.longitud(), result.telefono(), result.email(), result.esPrincipal(),
                result.permiteVentaOnline(), result.permiteDelivery(), result.perfilOperacion(),
                result.zonaHoraria(), result.estadoOperativo(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<EstablecimientoResponse> toEstablecimientoPage(
            PaginaResult<EstablecimientoResult> result) {
        return toPage(result, OrganizacionApiMapper::toResponse);
    }

    public static CrearAlmacenCommand toCommand(CrearAlmacenRequest request) {
        return new CrearAlmacenCommand(
                request.tenantId(), request.establecimientoId(), request.codigo(), request.nombre(),
                request.tipo(), request.permiteLotes(), request.permiteVencimiento(), request.permiteVenta(),
                request.permiteDespacho(), request.controlTemperatura(), request.temperaturaMinC(),
                request.temperaturaMaxC());
    }

    public static ActualizarAlmacenCommand toCommand(
            UUID almacenId, UUID tenantId, ActualizarAlmacenRequest request) {
        return new ActualizarAlmacenCommand(
                almacenId, tenantId, request.nombre(), request.tipo(), request.permiteLotes(),
                request.permiteVencimiento(), request.permiteVenta(), request.permiteDespacho(),
                request.controlTemperatura(), request.temperaturaMinC(), request.temperaturaMaxC(),
                request.activo());
    }

    public static AlmacenResponse toResponse(AlmacenResult result) {
        return new AlmacenResponse(
                result.id(), result.tenantId(), result.establecimientoId(), result.codigo(), result.nombre(),
                result.tipo(), result.permiteLotes(), result.permiteVencimiento(), result.permiteVenta(),
                result.permiteDespacho(), result.controlTemperatura(), result.temperaturaMinC(),
                result.temperaturaMaxC(), result.activo(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<AlmacenResponse> toAlmacenPage(PaginaResult<AlmacenResult> result) {
        return toPage(result, OrganizacionApiMapper::toResponse);
    }

    public static CrearTerminalPosCommand toCommand(CrearTerminalPosRequest request) {
        return new CrearTerminalPosCommand(
                request.tenantId(), request.establecimientoId(), request.codigo(), request.nombre(),
                request.serieBoletaDefecto(), request.serieFacturaDefecto(), request.numeroSerieEquipo(),
                request.hostname(), request.ipEquipo(), request.impresoraCodigo(),
                request.storeEdgeHabilitado());
    }

    public static ActualizarTerminalPosCommand toCommand(
            UUID terminalId, UUID tenantId, ActualizarTerminalPosRequest request) {
        return new ActualizarTerminalPosCommand(
                terminalId, tenantId, request.nombre(), request.serieBoletaDefecto(),
                request.serieFacturaDefecto(), request.numeroSerieEquipo(), request.hostname(),
                request.ipEquipo(), request.impresoraCodigo(), request.storeEdgeHabilitado(),
                request.estado());
    }

    public static TerminalPosResponse toResponse(TerminalPosResult result) {
        return new TerminalPosResponse(
                result.id(), result.tenantId(), result.establecimientoId(), result.codigo(), result.nombre(),
                result.serieBoletaDefecto(), result.serieFacturaDefecto(), result.numeroSerieEquipo(),
                result.hostname(), result.ipEquipo(), result.impresoraCodigo(), result.storeEdgeHabilitado(),
                result.estado(), result.createdAt(), result.updatedAt());
    }

    public static PaginaResponse<TerminalPosResponse> toTerminalPage(PaginaResult<TerminalPosResult> result) {
        return toPage(result, OrganizacionApiMapper::toResponse);
    }

    public static EstructuraCorporativaResponse toResponse(EstructuraCorporativaResult result) {
        return new EstructuraCorporativaResponse(
                result.asOf(), result.companies().stream().map(OrganizacionApiMapper::toResponse).toList());
    }

    private static <S, T> PaginaResponse<T> toPage(PaginaResult<S> result, Function<S, T> mapper) {
        return new PaginaResponse<>(
                result.items().stream().map(mapper).toList(), result.page(), result.size(),
                result.totalElements());
    }

    private static EmpresaNodoResponse toResponse(EmpresaNodoResult result) {
        return new EmpresaNodoResponse(
                result.id(), result.legalName(), result.tradeName(), result.status(),
                result.establishments().stream().map(OrganizacionApiMapper::toResponse).toList());
    }

    private static EstablecimientoNodoResponse toResponse(EstablecimientoNodoResult result) {
        return new EstablecimientoNodoResponse(
                result.id(), result.code(), result.name(), result.status(), result.timeZone(),
                result.warehouses().stream().map(OrganizacionApiMapper::toResponse).toList(),
                result.cashRegisters().stream().map(OrganizacionApiMapper::toResponse).toList());
    }

    private static NodoResponse toResponse(NodoResult result) {
        return new NodoResponse(result.id(), result.code(), result.name(), result.status());
    }
}
