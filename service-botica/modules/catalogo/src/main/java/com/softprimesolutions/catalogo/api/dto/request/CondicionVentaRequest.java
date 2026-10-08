package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CondicionVentaRequest(
        @NotBlank @Size(max = 30) String codigo,
        @NotBlank @Size(min = 2, max = 200) String denominacion,
        boolean requiereReceta,
        boolean requiereRetencion,
        @Size(max = 300) String fuente,
        @Size(max = 100) String versionFuente,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta) {
}
