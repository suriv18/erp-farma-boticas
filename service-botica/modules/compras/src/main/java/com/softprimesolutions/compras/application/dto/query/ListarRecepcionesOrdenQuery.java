package com.softprimesolutions.compras.application.dto.query;

import java.util.UUID;

public record ListarRecepcionesOrdenQuery(UUID tenantId, UUID ordenId, int page, int size) {
}
