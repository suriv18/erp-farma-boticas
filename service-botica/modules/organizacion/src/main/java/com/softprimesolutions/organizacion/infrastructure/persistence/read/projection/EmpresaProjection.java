package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;

import java.time.Instant;
import java.util.UUID;

public record EmpresaProjection(
        UUID uuidPublico,
        UUID tenantUuid,
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
