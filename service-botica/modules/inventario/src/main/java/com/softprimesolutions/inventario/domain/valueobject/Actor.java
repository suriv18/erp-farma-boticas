package com.softprimesolutions.inventario.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record Actor(UUID id) {

    public Actor {
        Objects.requireNonNull(id, "id es obligatorio");
    }

    public String codigo() {
        return id.toString();
    }
}
