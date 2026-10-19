package com.softprimesolutions.catalogo.api.dto.response;

public record TipoDocumentoIdentidadResponse(
        String codigo, String sigla, String denominacion, Integer max, Integer min, String estado) {
}
