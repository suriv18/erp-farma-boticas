package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record EstablecimientoId(UUID value) {
    public EstablecimientoId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
