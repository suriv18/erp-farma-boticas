package com.softprimesolutions.inventario.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record PosicionId(UUID value) {
    public PosicionId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
