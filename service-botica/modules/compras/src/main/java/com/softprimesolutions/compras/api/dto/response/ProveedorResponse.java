package com.softprimesolutions.compras.api.dto.response;

import java.util.UUID;

public record ProveedorResponse(
        UUID id,
        String tipoDocumento,
        String numeroDocumento,
        String razonSocial,
        String nombreComercial,
        String direccion,
        String ubigeo,
        String telefono,
        String email,
        String contactoNombre,
        String contactoTelefono,
        String contactoEmail,
        String condicionPagoDefault,
        int diasCreditoDefault,
        String monedaDefault,
        boolean esLaboratorio,
        boolean esImportador,
        boolean esDistribuidor,
        String calificacion,
        String estado) {
}
