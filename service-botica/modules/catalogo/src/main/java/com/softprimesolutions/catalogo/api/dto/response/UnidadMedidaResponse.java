package com.softprimesolutions.catalogo.api.dto.response;

public record UnidadMedidaResponse(
        String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente, String estado) {
}
