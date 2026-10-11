package com.softprimesolutions.organizacion.infrastructure.persistence.read.projection;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EstablecimientoProjection(
        UUID uuidPublico,
        UUID tenantUuid,
        UUID empresaUuid,
        String codigo,
        String nombre,
        String tipoEstablecimiento,
        String categoriaRegulatoriaCodigo,
        String codigoAnexoSunat,
        String codigoDigemid,
        String direccion,
        String ubigeo,
        String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        String telefono,
        String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        String perfilOperacion,
        String zonaHoraria,
        String estadoOperativo,
        Instant createdAt,
        Instant updatedAt) {
}
