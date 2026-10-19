package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ClasificacionControladaRequest(
        @NotBlank @Size(max = 40) String codigo,
        @NotBlank @Size(min = 2, max = 200) String denominacion,
        @Size(max = 300) String normaFuente,
        boolean requiereRecetaEspecial,
        boolean retieneReceta,
        Integer vigenciaRecetaDias) {
}
