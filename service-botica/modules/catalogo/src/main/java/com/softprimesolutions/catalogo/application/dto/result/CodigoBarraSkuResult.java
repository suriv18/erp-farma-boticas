package com.softprimesolutions.catalogo.application.dto.result;

import java.time.LocalDate;

public record CodigoBarraSkuResult(
        String codigoBarra, String tipoCodigo, boolean esPrincipal, LocalDate vigenteDesde,
        LocalDate vigenteHasta, String estado) {
}
