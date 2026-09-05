package com.softprimesolutions.organizacion.domain.empresa;

import com.softprimesolutions.shared.kernel.identity.EntityId;
import java.util.Objects;
import java.util.UUID;

/** Identidad fuerte de una empresa operadora. */
public record EmpresaOperadoraId(UUID value) implements EntityId<UUID> {

    public EmpresaOperadoraId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
