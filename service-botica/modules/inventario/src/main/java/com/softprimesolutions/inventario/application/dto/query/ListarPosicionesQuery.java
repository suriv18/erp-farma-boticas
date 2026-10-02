package com.softprimesolutions.inventario.application.dto.query;

import java.util.UUID;

public record ListarPosicionesQuery(
        UUID tenantId, UUID establecimientoId, UUID almacenId, UUID skuId, int page, int size) {
}
