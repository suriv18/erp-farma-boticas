package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record RubroComercialId(UUID value) {

    public RubroComercialId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
