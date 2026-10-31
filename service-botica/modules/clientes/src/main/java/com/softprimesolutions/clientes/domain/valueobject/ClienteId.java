package com.softprimesolutions.clientes.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record ClienteId(UUID value) {

    public ClienteId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
