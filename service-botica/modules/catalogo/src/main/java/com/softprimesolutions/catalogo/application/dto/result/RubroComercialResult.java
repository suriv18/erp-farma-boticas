package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record RubroComercialResult(
        UUID id, UUID tenantId, String codigo, String nombre, String descripcion, boolean esFarmaceutico,
        int orden, String estado) {
}
