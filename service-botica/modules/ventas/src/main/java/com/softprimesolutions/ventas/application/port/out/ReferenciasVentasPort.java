package com.softprimesolutions.ventas.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReferenciasVentasPort {

    Optional<TerminalRef> terminal(UUID tenantId, UUID terminalId);

    Optional<AlmacenRef> almacen(UUID tenantId, UUID almacenId);

    Map<UUID, SkuVentaRef> skus(UUID tenantId, Collection<UUID> skuIds);

    record TerminalRef(UUID id, UUID establecimientoId, String codigo, boolean operable) {
    }

    record AlmacenRef(UUID id, UUID establecimientoId, boolean operable) {
    }

    record SkuVentaRef(
            UUID id, String descripcion, String unidadVentaCodigo, boolean permiteFraccion, boolean operable) {
    }
}
