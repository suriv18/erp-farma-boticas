package com.softprimesolutions.inventario.api;

import java.util.UUID;

public record ReintegroVentaSolicitud(UUID tenantId, UUID ventaId, UUID actorId) {
}
