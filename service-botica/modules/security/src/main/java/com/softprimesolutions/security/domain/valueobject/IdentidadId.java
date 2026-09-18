package com.softprimesolutions.security.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record IdentidadId(UUID value) {

    public IdentidadId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
