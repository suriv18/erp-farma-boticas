package com.softprimesolutions.inventario.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record LoteId(UUID value) {
    public LoteId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
