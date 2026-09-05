package com.softprimesolutions.organizacion.api;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record EmpresaOperadoraRegistrada(
        UUID id,
        String identifierType,
        String identifierValue,
        String legalName,
        String tradeName,
        String status,
        Instant registeredAt) {

    public EmpresaOperadoraRegistrada {
        Objects.requireNonNull(id, "id es obligatorio");
        Objects.requireNonNull(identifierType, "identifierType es obligatorio");
        Objects.requireNonNull(identifierValue, "identifierValue es obligatorio");
        Objects.requireNonNull(legalName, "legalName es obligatorio");
        Objects.requireNonNull(status, "status es obligatorio");
        Objects.requireNonNull(registeredAt, "registeredAt es obligatorio");
    }
}
