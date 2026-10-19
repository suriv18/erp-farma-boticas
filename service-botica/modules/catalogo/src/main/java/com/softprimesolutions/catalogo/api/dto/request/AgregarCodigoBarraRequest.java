package com.softprimesolutions.catalogo.api.dto.request;

import jakarta.validation.constraints.NotBlank;
import java.time.LocalDate;

public record AgregarCodigoBarraRequest(
        @NotBlank String codigoBarra, String tipoCodigo, LocalDate vigenteDesde, LocalDate vigenteHasta) {
}
