package com.softprimesolutions.organizacion.application.dto.result;

import java.time.Instant;
import java.util.UUID;

public record EmpresaOperadoraResult(
        UUID id,
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
        boolean permiteVentaOnline,
        String estado,
        Instant createdAt,
        Instant updatedAt) {
}
