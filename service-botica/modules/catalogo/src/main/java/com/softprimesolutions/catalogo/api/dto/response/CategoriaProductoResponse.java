package com.softprimesolutions.catalogo.api.dto.response;

import java.util.UUID;

public record CategoriaProductoResponse(
        UUID id, UUID tenantId, UUID categoriaPadreId, String codigo, String nombre, String descripcion,
        int nivel, int orden, String estado) {
}
