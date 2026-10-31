package com.softprimesolutions.catalogo.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

/** Identificador público del tenant; la PK BIGINT permanece en persistencia. */
public record TenantId(UUID value) {

    public TenantId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
