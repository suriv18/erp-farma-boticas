package com.softprimesolutions.compras.application.dto.query;

import java.util.UUID;

public record ListarProveedoresQuery(UUID tenantId, String estado, String texto, int page, int size) {
}
