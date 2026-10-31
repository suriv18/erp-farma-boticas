package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record EmpresaOperadoraId(UUID value) {
    public EmpresaOperadoraId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
