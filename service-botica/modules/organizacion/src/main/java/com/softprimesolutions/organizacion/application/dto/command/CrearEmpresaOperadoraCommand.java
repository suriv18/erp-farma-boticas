package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record CrearEmpresaOperadoraCommand(
        UUID tenantId,
        String ruc,
        String razonSocial,
        String nombreComercial,
        String direccionFiscal,
        String ubigeoFiscal,
        String telefono,
        String email,
        String sitioWeb,
        String monedaFuncional,
        String zonaHoraria,
        boolean permiteVentaOnline) {
}
