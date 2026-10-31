package com.softprimesolutions.organizacion.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoRequest(@NotBlank String estado) {
}
