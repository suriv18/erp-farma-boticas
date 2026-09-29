package com.softprimesolutions.organizacion.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

public record CrearEstablecimientoCommand(
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
        String zonaHoraria) {
}
