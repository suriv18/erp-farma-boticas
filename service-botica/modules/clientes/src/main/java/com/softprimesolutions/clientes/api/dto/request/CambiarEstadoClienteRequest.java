package com.softprimesolutions.clientes.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CambiarEstadoClienteRequest(
        @NotBlank @Size(max = 20) String estado,
        @Size(max = 300) String motivo) {
}
