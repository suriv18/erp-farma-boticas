package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ObtenerEmpresaQuery(UUID tenantId, UUID empresaId) {
}
