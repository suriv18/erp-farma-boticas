package com.softprimesolutions.security.application.usecase.command;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.command.CrearUsuarioCommand;
import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CrearUsuarioHandlerTest {

    private static final UUID TENANT_ID = UUID.fromString("172e0f26-a765-46f3-841c-4a11407ccf5b");

    @Test
    void createsAUserWithoutRequiringAnExternalIdentityProvider() {
        var writePort = new FakeIamWritePort();
        var handler = new CrearUsuarioHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-06T10:00:00Z"));

        var result = handler.execute(new CrearUsuarioCommand(
                TENANT_ID, null, null, "Ada", "Lovelace", "ada",
                "admin@example.test", null, "Ada Lovelace", false, false));

        assertTrue(result.isSuccess());
        var created = result.getOrElse(error -> null);
        assertEquals("admin@example.test", created.email());
        assertEquals("Ada Lovelace", created.displayName());
    }

    @Test
    void failsWithConflictWhenEmailAlreadyExists() {
        var writePort = new FakeIamWritePort();
        writePort.saveOutcome = IamWritePort.SaveUsuarioOutcome.DUPLICATE_EMAIL;
        var handler = new CrearUsuarioHandler(writePort, new SequentialIds(), () -> Instant.parse("2026-09-06T10:00:00Z"));

        var result = handler.execute(new CrearUsuarioCommand(
                TENANT_ID, null, null, "Ada", "Lovelace", "ada",
                "admin@example.test", null, "Ada Lovelace", false, false));

        assertTrue(result.isFailure());
        assertEquals("SEC_EMAIL_DUPLICADO", result.fold(value -> null, error -> error.code()));
    }

    private static final class SequentialIds implements com.softprimesolutions.shared.application.port.IdentifierGenerator {
        private long sequence;

        @Override
        public UUID next() {
            return new UUID(0, ++sequence);
        }
    }

    private static final class FakeIamWritePort implements IamWritePort {
        private IamWritePort.SaveUsuarioOutcome saveOutcome = IamWritePort.SaveUsuarioOutcome.CREATED;

        @Override
        public SaveUsuarioOutcome save(Identidad identidad, Usuario user) {
            return saveOutcome;
        }

        @Override
        public SaveRolOutcome save(Rol role) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<Rol> findRole(UUID roleId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean allPermissionsExist(Set<String> permissionCodes) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean tenantExists(UUID tenantId) {
            return true;
        }

        @Override
        public boolean userBelongsToTenant(UUID userId, UUID tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean roleBelongsToTenant(UUID roleId, UUID tenantId) {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean scopeExists(UUID tenantId, AmbitoOrganizacional scope) {
            throw new UnsupportedOperationException();
        }

        @Override
        public SaveAssignmentOutcome save(AsignacionRol assignment) {
            throw new UnsupportedOperationException();
        }
    }
}
