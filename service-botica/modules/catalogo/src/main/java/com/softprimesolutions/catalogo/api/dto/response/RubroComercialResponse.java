package com.softprimesolutions.catalogo.api.dto.response;

import java.util.UUID;

public record RubroComercialResponse(
        UUID id, UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico,
        int orden, String estado) {
}
