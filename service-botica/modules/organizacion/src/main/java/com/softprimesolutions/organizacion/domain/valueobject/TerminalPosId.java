package com.softprimesolutions.organizacion.domain.valueobject;

import java.util.Objects;
import java.util.UUID;

public record TerminalPosId(UUID value) {
    public TerminalPosId {
        Objects.requireNonNull(value, "value es obligatorio");
    }
}
