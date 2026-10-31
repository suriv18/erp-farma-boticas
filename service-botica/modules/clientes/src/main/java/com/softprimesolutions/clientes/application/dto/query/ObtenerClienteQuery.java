package com.softprimesolutions.clientes.application.dto.query;

import java.util.UUID;

public record ObtenerClienteQuery(UUID tenantId, UUID clienteId) {
}
