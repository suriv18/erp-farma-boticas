package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record CategoriaProductoId(UUID value) {

    public CategoriaProductoId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
