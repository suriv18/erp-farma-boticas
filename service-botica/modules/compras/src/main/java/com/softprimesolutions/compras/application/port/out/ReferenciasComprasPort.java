package com.softprimesolutions.compras.application.port.out;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface ReferenciasComprasPort {

    Optional<EstablecimientoRef> establecimiento(UUID tenantId, UUID establecimientoId);

    Optional<AlmacenRef> almacen(UUID tenantId, UUID almacenId);

    Map<UUID, SkuRef> skus(UUID tenantId, Collection<UUID> skuIds);

    boolean existeUnidadMedida(String codigo);

    record EstablecimientoRef(UUID id, UUID empresaId, boolean operable) {
    }

    record AlmacenRef(UUID id, UUID establecimientoId, boolean operable) {
    }

    record SkuRef(UUID id, String descripcion, boolean operable) {
    }
}
