package com.softprimesolutions.compras.application.dto.result;

import java.util.UUID;

public record ProveedorResult(
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
