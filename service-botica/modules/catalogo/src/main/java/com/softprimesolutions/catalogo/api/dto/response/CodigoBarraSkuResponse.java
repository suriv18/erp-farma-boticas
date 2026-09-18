package com.softprimesolutions.catalogo.api.dto.response;

import java.time.LocalDate;

public record CodigoBarraSkuResponse(
        String codigoBarra, String tipoCodigo, boolean esPrincipal, LocalDate vigenteDesde,
        LocalDate vigenteHasta, String estado) {
}
