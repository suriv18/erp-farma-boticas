package com.softprimesolutions.organizacion.application.mapper;

import com.softprimesolutions.organizacion.application.dto.result.EmpresaOperadoraResult;
import com.softprimesolutions.organizacion.application.dto.result.EstablecimientoResult;
import com.softprimesolutions.organizacion.domain.model.EmpresaOperadora;
import com.softprimesolutions.organizacion.domain.model.Establecimiento;

public final class OrganizacionApplicationMapper {

    private OrganizacionApplicationMapper() {
    }

    public static EmpresaOperadoraResult toResult(EmpresaOperadora empresa) {
        return new EmpresaOperadoraResult(
                empresa.id().value(), empresa.tenantId().value(), empresa.ruc(), empresa.razonSocial(),
                empresa.nombreComercial(), empresa.direccionFiscal(), empresa.ubigeoFiscal(),
                empresa.telefono(), empresa.email(), empresa.sitioWeb(), empresa.monedaFuncional(),
                empresa.zonaHoraria(), empresa.permiteVentaOnline(), empresa.estado().name(),
                empresa.createdAt(), empresa.updatedAt());
    }

    public static EstablecimientoResult toResult(Establecimiento establecimiento) {
        return new EstablecimientoResult(
                establecimiento.id().value(), establecimiento.tenantId().value(),
                establecimiento.empresaId().value(), establecimiento.codigo(), establecimiento.nombre(),
                establecimiento.tipoEstablecimiento().name(), establecimiento.categoriaRegulatoriaCodigo(),
                establecimiento.codigoAnexoSunat(), establecimiento.codigoDigemid(),
                establecimiento.direccion(), establecimiento.ubigeo(), establecimiento.referencia(),
                establecimiento.latitud(), establecimiento.longitud(), establecimiento.telefono(),
                establecimiento.email(), establecimiento.esPrincipal(), establecimiento.permiteVentaOnline(),
                establecimiento.permiteDelivery(), establecimiento.perfilOperacion().name(),
                establecimiento.zonaHoraria(), establecimiento.estadoOperativo().name(),
                establecimiento.createdAt(), establecimiento.updatedAt());
    }
}
