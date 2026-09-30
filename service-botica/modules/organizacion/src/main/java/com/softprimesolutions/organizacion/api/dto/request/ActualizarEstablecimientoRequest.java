package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ActualizarEstablecimientoRequest(
        @NotBlank @Size(min = 2, max = 250) String nombre,
        @NotBlank String tipoEstablecimiento,
        @Size(max = 50) String categoriaRegulatoriaCodigo,
        @Size(min = 4, max = 4) String codigoAnexoSunat,
        @Size(max = 10) String codigoDigemid,
        @Size(max = 500) String direccion,
        @Size(max = 6) String ubigeo,
        @Size(max = 300) String referencia,
        BigDecimal latitud,
        BigDecimal longitud,
        @Size(max = 40) String telefono,
        @Size(max = 320) String email,
        boolean esPrincipal,
        boolean permiteVentaOnline,
        boolean permiteDelivery,
        @NotBlank String perfilOperacion,
        @Size(max = 80) String zonaHoraria) {
}
