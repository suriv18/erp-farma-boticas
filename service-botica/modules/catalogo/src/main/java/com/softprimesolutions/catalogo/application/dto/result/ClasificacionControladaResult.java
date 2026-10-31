package com.softprimesolutions.catalogo.application.dto.result;

public record ClasificacionControladaResult(
        String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
        boolean retieneReceta, Integer vigenciaRecetaDias, String estado) {
}
