package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record MarcaId(UUID value) {

    public MarcaId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
