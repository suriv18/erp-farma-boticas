package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record MarcaResult(
        UUID id, UUID tenantId, String codigo, String nombre, String descripcion, String estado) {
}
