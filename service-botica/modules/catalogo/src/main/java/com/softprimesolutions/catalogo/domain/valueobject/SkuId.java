package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record SkuId(UUID value) {

    public SkuId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
