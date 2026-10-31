package com.softprimesolutions.clientes.application.dto.query;

import java.util.UUID;

public record ListarClientesQuery(UUID tenantId, String search, int page, int size) {
}
