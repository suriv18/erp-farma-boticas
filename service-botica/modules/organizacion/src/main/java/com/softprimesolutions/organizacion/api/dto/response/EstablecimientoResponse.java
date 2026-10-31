package com.softprimesolutions.organizacion.api.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record EstablecimientoResponse(
        UUID id,
        UUID tenantId,
        UUID empresaId,
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
