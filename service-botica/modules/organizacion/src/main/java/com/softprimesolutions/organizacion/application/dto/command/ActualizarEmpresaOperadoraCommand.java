package com.softprimesolutions.organizacion.application.dto.command;

import java.util.UUID;

public record ActualizarEmpresaOperadoraCommand(
        UUID empresaId,
        UUID tenantId,
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
