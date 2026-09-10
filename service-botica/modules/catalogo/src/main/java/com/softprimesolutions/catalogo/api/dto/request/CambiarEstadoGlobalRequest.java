package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoGlobalRequest(@NotBlank String status) {
}
