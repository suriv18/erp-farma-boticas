package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record AlmacenId(UUID value) {
    public AlmacenId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
