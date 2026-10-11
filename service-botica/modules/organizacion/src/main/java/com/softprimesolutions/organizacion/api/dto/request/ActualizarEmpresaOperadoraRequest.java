package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ActualizarEmpresaOperadoraRequest(
        @NotBlank @Size(min = 2, max = 300) String razonSocial,
        @Size(max = 300) String nombreComercial,
        @Size(max = 500) String direccionFiscal,
        @Size(max = 6) String ubigeoFiscal,
        @Size(max = 40) String telefono,
        @Size(max = 320) String email,
        @Size(max = 300) String sitioWeb,
        @Size(min = 3, max = 3) String monedaFuncional,
        @Size(max = 80) String zonaHoraria,
        boolean permiteVentaOnline) {
}
