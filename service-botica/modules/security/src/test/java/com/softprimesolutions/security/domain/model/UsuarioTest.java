package com.softprimesolutions.security.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void registersAndNormalizesAnExternalIdentityProfile() {
        var result = Usuario.register(
                new UsuarioId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5")),
                "oidc",
                " https://id.example.test/tenant ",
                " subject-123 ",
                " ADMIN@EXAMPLE.TEST ", null, null, "Ada", "Lovelace", "ada",
                " ADMIN@EXAMPLE.TEST ", null,
                "  Ada   Lovelace  ",
                false,
                false,
                Instant.parse("2026-09-02T20:00:00Z"));

        assertTrue(result.isSuccess());
        var user = result.getOrElse(error -> null);
        assertEquals("https://id.example.test/tenant", user.identity().issuer());
        assertEquals("subject-123", user.identity().subject());
        assertEquals("admin@example.test", user.email());
        assertEquals("Ada Lovelace", user.displayName());
        assertEquals(EstadoUsuario.ACTIVO, user.status());
    }

    @Test
    void rejectsAnInvalidEmailWithoutThrowingAnExpectedException() {
        var result = Usuario.register(
                new UsuarioId(UUID.randomUUID()),
                new TenantId(UUID.randomUUID()),
                "oidc",
                "https://id.example.test/tenant",
                "subject-123",
                null, null, null, "Ada", "Lovelace", "ada",
                "invalid-email", null, "Ada Lovelace", false, false,
                Instant.parse("2026-09-02T20:00:00Z"));

        assertTrue(result.isFailure());
        assertEquals("SEC_USUARIO_INVALIDO", result.fold(value -> null, error -> error.code()));
    }
}
