package com.softprimesolutions.organizacion.application.dto.query;

import java.util.UUID;

public record ListarEmpresasQuery(UUID tenantId, String search, int page, int size) {
}
