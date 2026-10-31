package com.softprimesolutions.inventario.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record TenantId(UUID value) {
    public TenantId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
