package com.softprimesolutions.security.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.command.ActualizarRolCommand;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional;
import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ActualizarRolHandlerTest {

    private static final UUID ROLE_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");
    private static final Instant NOW = Instant.parse("2026-09-27T20:00:00Z");

    @Test
    void updatesRoleDetailsWhenCodeIsUnchanged() {
        var port = new StubWritePort(true, false);
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL", "Administrador local actualizado", "Descripción", "ALMACEN"));

        assertTrue(result.isSuccess());
        var updated = result.fold(value -> value, error -> null);
        assertEquals("Administrador local actualizado", updated.name());
        assertEquals("ALMACEN", updated.roleType());
        assertTrue(port.saved);
    }

    @Test
    void updatesRoleDetailsWhenCodeIsNull() {
        var port = new StubWritePort(true, false);
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, null, "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_INVALIDO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void updatesRoleDetailsWhenCodeChangesWithoutDuplicate() {
        var port = new StubWritePort(true, false);
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL_2", "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isSuccess());
        assertEquals("ADMIN_LOCAL_2", result.fold(value -> value.code(), error -> null));
    }

    @Test
    void rejectsUpdateWhenRoleDoesNotExist() {
        var port = new StubWritePort(false, false);
        port.roleFound = false;
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL", "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsUpdateWhenRoleIsSystemRole() {
        var port = new StubWritePort(true, false);
        port.systemRole = true;
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL", "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_SISTEMA_NO_EDITABLE", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void rejectsUpdateWhenNewCodeIsDuplicate() {
        var port = new StubWritePort(true, true);
        var handler = new ActualizarRolHandler(port, () -> NOW);

        var result = handler.execute(new ActualizarRolCommand(
                ROLE_ID, "ADMIN_LOCAL_2", "Administrador local", null, "ESTABLECIMIENTO"));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_CODIGO_DUPLICADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class StubWritePort implements IamWritePort {

        private final boolean allowSave;
        private final boolean duplicateCode;
        private boolean roleFound = true;
        private boolean systemRole;
        private boolean saved;

        private StubWritePort(boolean allowSave, boolean duplicateCode) {
            this.allowSave = allowSave;
            this.duplicateCode = duplicateCode;
        }

        @Override
        public SaveUsuarioOutcome save(Identidad identidad, Usuario user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveRolOutcome save(Rol role) {
            if (!allowSave) throw new IllegalStateException("save no debería invocarse en este escenario");
            saved = true;
            return SaveRolOutcome.UPDATED;
        }

        @Override
        public SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Rol> findRole(UUID roleId) {
            if (!roleFound) return Optional.empty();
            return Rol.create(
                    new RolId(roleId), new TenantId(TENANT_ID), "ADMIN_LOCAL", "Administrador local", null,
                    "ESTABLECIMIENTO", systemRole, Instant.parse("2026-09-01T00:00:00Z"))
                    .fold(Optional::of, error -> Optional.empty());
        }

        @Override
        public boolean allPermissionsExist(Set<String> permissionCodes) {
            return false;
        }

        @Override
        public boolean tenantExists(UUID tenantId) {
            return false;
        }

        @Override
        public boolean userBelongsToTenant(UUID userId, UUID tenantId) {
            return false;
        }

        @Override
        public boolean roleBelongsToTenant(UUID roleId, UUID tenantId) {
            return false;
        }

        @Override
        public boolean existsActiveRoleWithCode(UUID tenantId, String code, UUID excludingRoleId) {
            return duplicateCode;
        }

        @Override
        public boolean scopeExists(UUID tenantId, AmbitoOrganizacional scope) {
            return false;
        }

        @Override
        public SaveAssignmentOutcome save(AsignacionRol assignment) {
            throw new UnsupportedOperationException();
        }
    }
}
