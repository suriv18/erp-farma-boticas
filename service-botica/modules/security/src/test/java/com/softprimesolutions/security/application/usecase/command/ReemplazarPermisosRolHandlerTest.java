package com.softprimesolutions.security.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.command.ReemplazarPermisosRolCommand;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ReemplazarPermisosRolHandlerTest {

    private static final UUID ROLE_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");
    private static final Instant NOW = Instant.parse("2026-09-02T20:00:00Z");

    @Test
    void replacesOnlyPermissionsThatBelongToTheCatalog() {
        var port = new StubWritePort(true);
        var handler = new ReemplazarPermisosRolHandler(port, () -> NOW);

        var result = handler.execute(new ReemplazarPermisosRolCommand(
                ROLE_ID, Set.of("seguridad.usuarios.consultar"), "admin"));

        assertTrue(result.isSuccess());
        assertEquals(Set.of("seguridad.usuarios.consultar"),
                result.getOrElse(error -> null).permissionCodes());
        assertTrue(port.saved);
    }

    @Test
    void rejectsUnknownPermissionCodes() {
        var port = new StubWritePort(false);
        var handler = new ReemplazarPermisosRolHandler(port, () -> NOW);

        var result = handler.execute(new ReemplazarPermisosRolCommand(
                ROLE_ID, Set.of("seguridad.inexistente.ejecutar"), "admin"));

        assertTrue(result.isFailure());
        assertEquals("SEC_PERMISO_DESCONOCIDO", result.fold(value -> null, error -> error.code()));
    }

    private static final class StubWritePort implements IamWritePort {

        private final boolean permissionsExist;
        private boolean saved;

        private StubWritePort(boolean permissionsExist) {
            this.permissionsExist = permissionsExist;
        }

        @Override
        public SaveUsuarioOutcome save(Identidad identidad, Usuario user) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveRolOutcome save(Rol role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt) {
            saved = true;
            return SaveRolOutcome.UPDATED;
        }

        @Override
        public Optional<Rol> findRole(UUID roleId) {
            return Rol.create(
                    new RolId(roleId), new TenantId(TENANT_ID), "ADMIN_LOCAL", "Administrador local", null,
                    "ESTABLECIMIENTO", false, NOW)
                    .fold(Optional::of, error -> Optional.empty());
        }

        @Override
        public boolean allPermissionsExist(Set<String> permissionCodes) {
            return permissionsExist;
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
        public boolean scopeExists(UUID tenantId, AmbitoOrganizacional scope) {
            return false;
        }

        @Override
        public SaveAssignmentOutcome save(AsignacionRol assignment) {
            throw new UnsupportedOperationException();
        }
    }
}
