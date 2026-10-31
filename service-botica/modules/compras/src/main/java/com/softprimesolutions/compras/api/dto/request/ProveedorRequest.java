package com.softprimesolutions.compras.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ProveedorRequest(
        @Size(max = 2) String tipoDocumento,
        @NotBlank @Size(max = 15) String numeroDocumento,
        @NotBlank @Size(max = 300) String razonSocial,
        @Size(max = 300) String nombreComercial,
        @Size(max = 500) String direccion,
        @Size(max = 6) String ubigeo,
        @Size(max = 40) String telefono,
        @Size(max = 254) String email,
        @Size(max = 180) String contactoNombre,
        @Size(max = 40) String contactoTelefono,
        @Size(max = 254) String contactoEmail,
        @Size(max = 80) String condicionPagoDefault,
        @PositiveOrZero Integer diasCreditoDefault,
        @Size(max = 3) String monedaDefault,
        Boolean esLaboratorio,
        Boolean esImportador,
        Boolean esDistribuidor,
        @Size(max = 30) String calificacion) {
}
