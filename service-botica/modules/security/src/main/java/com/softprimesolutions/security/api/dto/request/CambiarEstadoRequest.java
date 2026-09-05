package com.softprimesolutions.security.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoRequest(@NotBlank String status) {
}
