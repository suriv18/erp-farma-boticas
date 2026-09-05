package com.softprimesolutions.security.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record RolId(UUID value) {

    public RolId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
