package com.softprimesolutions.catalogo.api.dto.response;

public record ClasificacionControladaResponse(
        String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
        boolean retieneReceta, Integer vigenciaRecetaDias, String estado) {
}
