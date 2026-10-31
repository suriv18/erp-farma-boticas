package com.softprimesolutions.catalogo.application.dto.result;

public record UnidadMedidaResult(
        String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente, String estado) {
}
