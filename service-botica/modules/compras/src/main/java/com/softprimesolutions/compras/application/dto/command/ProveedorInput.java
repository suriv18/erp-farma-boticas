package com.softprimesolutions.compras.application.dto.command;

public record ProveedorInput(
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
        Integer diasCreditoDefault,
        String monedaDefault,
        Boolean esLaboratorio,
        Boolean esImportador,
        Boolean esDistribuidor,
        String calificacion) {
}
