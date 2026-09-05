package com.softprimesolutions.security.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record UsuarioId(UUID value) {

    public UsuarioId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
