package com.softprimesolutions.organizacion.infrastructure.persistence.write.mapper;

import com.softprimesolutions.organizacion.domain.model.Almacen;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.AlmacenJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EmpresaOperadoraJpaEntity;
import com.softprimesolutions.organizacion.infrastructure.persistence.write.entity.EstablecimientoJpaEntity;

public final class OrganizacionWriteMapper {

    private OrganizacionWriteMapper() {
    }

    public static String activoFlag(boolean activo) {
        return activo ? "1" : "0";
    }

    public static EmpresaOperadoraJpaEntity toEntity(EmpresaOperadora empresa, Long tenantId) {
        return new EmpresaOperadoraJpaEntity(
                empresa.id().value(), tenantId, empresa.ruc(), empresa.razonSocial(),
                empresa.nombreComercial(), empresa.direccionFiscal(), empresa.ubigeoFiscal(),
                empresa.telefono(), empresa.email(), empresa.sitioWeb(), empresa.monedaFuncional(),
                empresa.zonaHoraria(), empresa.permiteVentaOnline(), empresa.estado().name(),
                empresa.createdAt(), empresa.updatedAt());
    }

    public static EstablecimientoJpaEntity toEntity(
            Establecimiento establecimiento, Long tenantId, Long empresaId) {
        return new EstablecimientoJpaEntity(
                establecimiento.id().value(), tenantId, empresaId, establecimiento.codigo(),
                establecimiento.nombre(), establecimiento.tipoEstablecimiento().name(),
                establecimiento.categoriaRegulatoriaCodigo(), establecimiento.codigoAnexoSunat(),
                establecimiento.codigoDigemid(), establecimiento.direccion(), establecimiento.ubigeo(),
                establecimiento.referencia(), establecimiento.latitud(), establecimiento.longitud(),
                establecimiento.telefono(), establecimiento.email(), establecimiento.esPrincipal(),
                establecimiento.permiteVentaOnline(), establecimiento.permiteDelivery(),
                establecimiento.perfilOperacion().name(), establecimiento.zonaHoraria(),
                establecimiento.estadoOperativo().name(), establecimiento.createdAt(),
                establecimiento.updatedAt());
    }

    public static AlmacenJpaEntity toEntity(
            Almacen almacen, Long tenantId, Long empresaId, Long establecimientoId) {
        return new AlmacenJpaEntity(
                almacen.id().value(), tenantId, empresaId, establecimientoId, almacen.codigo(),
                almacen.nombre(), almacen.tipo().name(), almacen.permiteLotes(),
                almacen.permiteVencimiento(), almacen.permiteVenta(), almacen.permiteDespacho(),
                almacen.controlTemperatura(), almacen.temperaturaMinC(), almacen.temperaturaMaxC(),
                activoFlag(almacen.activo()), almacen.createdAt(), almacen.updatedAt());
    }
}
