package com.softprimesolutions.security.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class UsuarioTest {

    @Test
    void registersAMembershipForATenant() {
        var result = Usuario.register(
                new UsuarioId(UUID.fromString("98a1587e-27ef-4077-befd-6f5af4901589")),
                new TenantId(UUID.fromString("a92adf67-70e7-4cc1-bb05-ff074df7fdf5")),
                new IdentidadId(UUID.fromString("11111111-1111-1111-1111-111111111111")),
                "  Ada   Lovelace  ",
                false,
                false,
                Instant.parse("2026-09-02T20:00:00Z"));

        assertTrue(result.isSuccess());
        var user = result.getOrElse(error -> null);
        assertEquals("Ada Lovelace", user.displayName());
        assertEquals(EstadoUsuario.ACTIVO, user.status());
    }

    @Test
    void requiresTenantAndIdentidadId() {
        var missingTenant = Usuario.register(
                new UsuarioId(UUID.randomUUID()), null,
                new IdentidadId(UUID.randomUUID()), "Ada Lovelace", false, false,
                Instant.parse("2026-09-02T20:00:00Z"));
        assertTrue(missingTenant.isFailure());
        assertEquals("SEC_USUARIO_INVALIDO", missingTenant.fold(value -> null, error -> error.code()));

        var missingIdentidad = Usuario.register(
                new UsuarioId(UUID.randomUUID()), new TenantId(UUID.randomUUID()),
                null, "Ada Lovelace", false, false,
                Instant.parse("2026-09-02T20:00:00Z"));
        assertTrue(missingIdentidad.isFailure());
    }
}
