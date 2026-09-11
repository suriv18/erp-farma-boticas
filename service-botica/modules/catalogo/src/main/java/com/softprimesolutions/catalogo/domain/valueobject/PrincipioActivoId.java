package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record PrincipioActivoId(UUID value) {

    public PrincipioActivoId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
