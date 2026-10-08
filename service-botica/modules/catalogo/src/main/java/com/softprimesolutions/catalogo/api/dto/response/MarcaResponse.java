package com.softprimesolutions.catalogo.api.dto.response;

import java.util.UUID;

public record MarcaResponse(UUID id, UUID tenantId, String codigo, String nombre, String descripcion, String estado) {
}
