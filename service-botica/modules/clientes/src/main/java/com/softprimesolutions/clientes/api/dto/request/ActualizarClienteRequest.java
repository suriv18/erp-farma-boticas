package com.softprimesolutions.clientes.api.dto.request;

import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ActualizarClienteRequest(
        @Size(max = 150) String nombres,
        @Size(max = 180) String apellidos,
        @Size(max = 300) String razonSocial,
        @Size(max = 320) String email,
        @Size(max = 40) String telefono,
        LocalDate fechaNacimiento) {
}
