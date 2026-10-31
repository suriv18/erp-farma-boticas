package com.softprimesolutions.clientes.application.dto.result;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClienteResult(
        UUID id,
        UUID tenantId,
        String tipoCliente,
        String tipoDocumento,
        String numeroDocumento,
        String nombres,
        String apellidos,
        String razonSocial,
        String email,
        String telefono,
        LocalDate fechaNacimiento,
        String estado,
        String motivoBloqueo,
        Instant createdAt,
        Instant updatedAt) {
}
