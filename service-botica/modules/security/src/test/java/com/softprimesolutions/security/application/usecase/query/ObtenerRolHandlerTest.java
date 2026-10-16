package com.softprimesolutions.security.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.query.ObtenerRolQuery;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerRolHandlerTest {

    private static final UUID ROLE_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");

    @Test
    void returnsRoleWhenFound() {
        var sampleRole = new RolResult(
                ROLE_ID, TENANT_ID, "ADMIN_LOCAL", "Administrador local", null, "ESTABLECIMIENTO",
                false, Set.of(), "ACTIVO", Instant.parse("2026-09-01T00:00:00Z"), null);
        var handler = new ObtenerRolHandler(new StubReadPort(Optional.of(sampleRole)));

        var result = handler.execute(new ObtenerRolQuery(TENANT_ID, ROLE_ID));

        assertTrue(result.isSuccess());
        assertEquals("ADMIN_LOCAL", result.fold(value -> value.code(), error -> null));
    }

    @Test
    void rejectsWhenRoleIsNotFound() {
        var handler = new ObtenerRolHandler(new StubReadPort(Optional.empty()));

        var result = handler.execute(new ObtenerRolQuery(TENANT_ID, ROLE_ID));

        assertTrue(result.isFailure());
        assertEquals("SEC_ROL_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    private record StubReadPort(Optional<RolResult> role) implements IamReadPort {

        @Override
        public PaginaResult<UsuarioResult> findUsers(UUID tenantId, String search, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<RolResult> findRoles(UUID tenantId, String search, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public PaginaResult<PermisoResult> findPermissions(String search, int page, int size) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<RolResult> findRoleById(UUID tenantId, UUID roleId) {
            return role;
        }
    }
}
