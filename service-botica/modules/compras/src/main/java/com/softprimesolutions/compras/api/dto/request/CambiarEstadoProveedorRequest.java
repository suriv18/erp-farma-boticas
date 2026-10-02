package com.softprimesolutions.compras.api.dto.request;

import jakarta.validation.constraints.NotBlank;

public record CambiarEstadoProveedorRequest(@NotBlank String estado) {
}
