package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.UUID;

public record CrearEmpresaOperadoraRequest(
        @NotNull UUID tenantId,
        @NotBlank @Size(min = 11, max = 11) String ruc,
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
