package com.softprimesolutions.clientes.api.dto.response;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record ClienteResponse(
        UUID id,
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
        Instant createdAt) {
}
