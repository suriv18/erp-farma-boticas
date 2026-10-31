package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record CategoriaProductoResult(
        UUID id, UUID tenantId, UUID categoriaPadreId, String codigo, String nombre, String descripcion,
        int nivel, int orden, String estado) {
}
