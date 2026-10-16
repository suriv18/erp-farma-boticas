package com.softprimesolutions.security.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IdentidadTest {

    @Test
    void registersAndNormalizesAPersonProfile() {
        var result = Identidad.register(
                new IdentidadId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                null, null, "Ada", "Lovelace", "ada",
                " ADMIN@EXAMPLE.TEST ", null,
                Instant.parse("2026-09-02T20:00:00Z"));

        assertTrue(result.isSuccess());
        var identity = result.getOrElse(error -> null);
        assertEquals("admin@example.test", identity.email());
        assertEquals("ada", identity.username());
        assertEquals("Ada", identity.firstNames());
        assertEquals("Lovelace", identity.lastNames());
    }

    @Test
    void rejectsAnInvalidEmailWithoutThrowingAnExpectedException() {
        var result = Identidad.register(
                new IdentidadId(UUID.randomUUID()),
                null, null, "Ada", "Lovelace", "ada",
                "invalid-email", null,
                Instant.parse("2026-09-02T20:00:00Z"));

        assertTrue(result.isFailure());
        assertEquals("SEC_IDENTIDAD_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void requiresIdAndCreatedAt() {
        var missingId = Identidad.register(
                null, null, null, "Ada", "Lovelace", "ada",
                "admin@example.test", null, Instant.parse("2026-09-02T20:00:00Z"));
        assertTrue(missingId.isFailure());

        var missingCreatedAt = Identidad.register(
                new IdentidadId(UUID.randomUUID()),
                null, null, "Ada", "Lovelace", "ada",
                "admin@example.test", null, null);
        assertTrue(missingCreatedAt.isFailure());
    }
}
