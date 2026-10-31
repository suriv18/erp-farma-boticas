package com.softprimesolutions.clientes.application.dto.command;

import java.time.LocalDate;
import java.util.UUID;

public record RegistrarClienteCommand(
        UUID tenantId,
        String tipoCliente,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String razonSocial,
        String email,
        String telefono,
        LocalDate fechaNacimiento) {
}
