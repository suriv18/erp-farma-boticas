package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record ProductoReguladoId(UUID value) {

    public ProductoReguladoId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
