package com.softprimesolutions.clientes.application.dto.command;

import java.time.LocalDate;
import java.util.UUID;

public record ActualizarClienteCommand(
        UUID tenantId,
        UUID clienteId,
        String nombres,
        String apellidos,
        String razonSocial,
        String email,
        String telefono,
        LocalDate fechaNacimiento) {
}
