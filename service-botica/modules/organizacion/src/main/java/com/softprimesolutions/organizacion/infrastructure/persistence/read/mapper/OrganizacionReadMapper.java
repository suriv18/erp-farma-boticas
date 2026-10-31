package com.softprimesolutions.organizacion.infrastructure.persistence.read.mapper;

import com.softprimesolutions.organizacion.application.dto.result.AlmacenResult;
import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.application.dto.result.TerminalPosResult;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.AlmacenProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EmpresaProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.EstablecimientoProjection;
import com.softprimesolutions.organizacion.infrastructure.persistence.read.projection.TerminalProjection;

public final class OrganizacionReadMapper {

    private OrganizacionReadMapper() {
    }

    public static EmpresaOperadoraResult toResult(EmpresaProjection projection) {
        return new EmpresaOperadoraResult(
                projection.uuidPublico(), projection.tenantUuid(), projection.ruc(), projection.razonSocial(),
                projection.nombreComercial(), projection.direccionFiscal(), projection.ubigeoFiscal(),
                projection.telefono(), projection.email(), projection.sitioWeb(), projection.monedaFuncional(),
                projection.zonaHoraria(), projection.permiteVentaOnline(), projection.estado(),
                projection.createdAt(), projection.updatedAt());
    }

    public static EstablecimientoResult toResult(EstablecimientoProjection projection) {
        return new EstablecimientoResult(
                projection.uuidPublico(), projection.tenantUuid(), projection.empresaUuid(),
                projection.codigo(), projection.nombre(), projection.tipoEstablecimiento(),
                projection.categoriaRegulatoriaCodigo(), projection.codigoAnexoSunat(),
                projection.codigoDigemid(), projection.direccion(), projection.ubigeo(),
                projection.referencia(), projection.latitud(), projection.longitud(), projection.telefono(),
                projection.email(), projection.esPrincipal(), projection.permiteVentaOnline(),
                projection.permiteDelivery(), projection.perfilOperacion(), projection.zonaHoraria(),
                projection.estadoOperativo(), projection.createdAt(), projection.updatedAt());
    }

    public static AlmacenResult toResult(AlmacenProjection projection) {
        return new AlmacenResult(
                projection.uuidPublico(), projection.tenantUuid(), projection.establecimientoUuid(),
                projection.codigo(), projection.nombre(), projection.tipo(), projection.permiteLotes(),
                projection.permiteVencimiento(), projection.permiteVenta(), projection.permiteDespacho(),
                projection.controlTemperatura(), projection.temperaturaMinC(), projection.temperaturaMaxC(),
                projection.activo(), projection.createdAt(), projection.updatedAt());
    }

    public static TerminalPosResult toResult(TerminalProjection projection) {
        return new TerminalPosResult(
                projection.uuidPublico(), projection.tenantUuid(), projection.establecimientoUuid(),
                projection.codigo(), projection.nombre(), projection.serieBoletaDefecto(),
                projection.serieFacturaDefecto(), projection.numeroSerieEquipo(), projection.hostname(),
                projection.ipEquipo(), projection.impresoraCodigo(), projection.storeEdgeHabilitado(),
                projection.estado(), projection.createdAt(), projection.updatedAt());
    }

    public static String activeOrInactive(boolean active) {
        return active ? "ACTIVE" : "INACTIVE";
    }

    public static String activeOrInactive(String estado) {
        return activeOrInactive("ACTIVO".equals(estado));
    }

    public static String establishmentStatus(String estadoOperativo) {
        return "SUSPENDIDO".equals(estadoOperativo) ? "SUSPENDED" : activeOrInactive(estadoOperativo);
    }
}
