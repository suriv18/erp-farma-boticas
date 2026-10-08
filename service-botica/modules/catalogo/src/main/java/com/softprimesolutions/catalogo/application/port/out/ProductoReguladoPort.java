package com.softprimesolutions.catalogo.application.port.out;

import com.softprimesolutions.catalogo.domain.model.ProductoRegulado;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface ProductoReguladoPort {

    SaveOutcome save(ProductoRegulado productoRegulado);

    Optional<ProductoRegulado> findById(UUID productoReguladoId);

    boolean changeStatus(UUID productoReguladoId, String status, Instant changedAt);

    enum SaveOutcome {
        CREATED, UPDATED, NOT_FOUND, FORMA_FARMACEUTICA_NOT_FOUND, VIA_ADMINISTRACION_NOT_FOUND,
        UNIDAD_MEDIDA_NOT_FOUND, CONDICION_VENTA_NOT_FOUND, CLASIFICACION_CONTROLADA_NOT_FOUND,
        PRINCIPIO_ACTIVO_NOT_FOUND
    }
}
