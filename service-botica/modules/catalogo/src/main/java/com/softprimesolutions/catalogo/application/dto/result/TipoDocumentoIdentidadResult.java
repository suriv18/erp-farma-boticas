package com.softprimesolutions.catalogo.application.dto.result;

public record TipoDocumentoIdentidadResult(
        String codigo, String sigla, String denominacion, Integer max, Integer min, String estado) {
}
