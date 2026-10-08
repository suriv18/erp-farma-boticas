package com.softprimesolutions.security.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RolTest {

    private static final UUID ROLE_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");
    private static final Instant CREATED_AT = Instant.parse("2026-09-01T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-09-27T00:00:00Z");

    private Rol createNonSystemRole() {
        return Rol.create(
                new RolId(ROLE_ID), new TenantId(TENANT_ID), "ADMIN_LOCAL", "Administrador local", null,
                "ESTABLECIMIENTO", false, CREATED_AT)
                .getOrElse(error -> null);
    }

    private Rol createSystemRole() {
        return Rol.create(
                new RolId(ROLE_ID), new TenantId(TENANT_ID), "SUPERADMIN", "Superadministrador", null,
                "GLOBAL", true, CREATED_AT)
                .getOrElse(error -> null);
    }

    @Test
    void updatesDetailsWhenDataIsValid() {
        var role = createNonSystemRole();

        var result = role.updateDetails(
                "ADMIN_LOCAL_2", "Administrador local actualizado", "Descripción nueva",
                "ALMACEN", UPDATED_AT);

        assertTrue(result.isSuccess());
        var updated = result.getOrElse(error -> null);
        assertEquals("ADMIN_LOCAL_2", updated.code());
        assertEquals("Administrador local actualizado", updated.name());
        assertEquals("Descripción nueva", updated.description());
        assertEquals(TipoRol.ALMACEN, updated.roleType());
        assertEquals(UPDATED_AT, updated.updatedAt());
        assertEquals(ROLE_ID, updated.id().value());
        assertEquals(Set.of(), updated.permissionCodes());
        assertEquals(EstadoRol.ACTIVO, updated.status());
    }

    @Test
    void rejectsUpdateWhenRoleIsSystemRole() {
        var role = createSystemRole();

        var result = role.updateDetails(
                "SUPERADMIN_2", "Otro nombre", null, "GLOBAL", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_SISTEMA_NO_EDITABLE", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsUpdateWhenCodeIsInvalid() {
        var role = createNonSystemRole();

        var result = role.updateDetails("ab", "Nombre válido", null, "ESTABLECIMIENTO", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_INVALIDO", result.fold(value -> null, error -> error.code()));
        assertEquals("code", result.fold(value -> null, error -> error.metadata().get("field")));
    }

    @Test
    void rejectsUpdateWhenNameIsTooShort() {
        var role = createNonSystemRole();

        var result = role.updateDetails("ADMIN_LOCAL", "A", null, "ESTABLECIMIENTO", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("name", result.fold(value -> null, error -> error.metadata().get("field")));
    }

    @Test
    void rejectsUpdateWhenDescriptionExceedsMaxLength() {
        var role = createNonSystemRole();

        var result = role.updateDetails(
                "ADMIN_LOCAL", "Nombre válido", "x".repeat(501), "ESTABLECIMIENTO", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("description", result.fold(value -> null, error -> error.metadata().get("field")));
    }

    @Test
    void rejectsUpdateWhenRoleTypeIsInvalid() {
        var role = createNonSystemRole();

        var result = role.updateDetails("ADMIN_LOCAL", "Nombre válido", null, "NO_EXISTE", UPDATED_AT);

        assertTrue(result.isFailure());
        assertEquals("roleType", result.fold(value -> null, error -> error.metadata().get("field")));
    }

    @Test
    void rejectsUpdateWhenUpdatedAtIsNull() {
        var role = createNonSystemRole();

        var result = role.updateDetails("ADMIN_LOCAL", "Nombre válido", null, "ESTABLECIMIENTO", null);

        assertTrue(result.isFailure());
        assertEquals("updatedAt", result.fold(value -> null, error -> error.metadata().get("field")));
    }
}
