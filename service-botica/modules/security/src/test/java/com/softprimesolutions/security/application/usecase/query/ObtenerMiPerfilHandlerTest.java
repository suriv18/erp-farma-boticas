package com.softprimesolutions.security.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.query.ObtenerMiPerfilQuery;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PerfilResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ObtenerMiPerfilHandlerTest {

    private static final UUID USER_ID = UUID.fromString("248b31ce-cbd5-4ab7-9daf-c78c292b2d32");
    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");

    @Test
    void returnsTheProfileOfTheAuthenticatedUser() {
        var profile = new PerfilResult(
                USER_ID, TENANT_ID, "Luis Paredes Soto", "crit001.manana1", "crit001.manana1@boticas.pe",
                List.of(new PerfilResult.Asignacion(
                        "CAJERO", "Cajero de establecimiento", "ESTABLECIMIENTO", "Botica Crítica Norte")));
        var handler = new ObtenerMiPerfilHandler(new StubReadPort(Optional.of(profile)));

        var result = handler.execute(new ObtenerMiPerfilQuery(TENANT_ID, USER_ID));

        assertTrue(result.isSuccess());
        assertEquals(profile, result.getOrElse(error -> null));
    }

    @Test
    void rejectsWhenTheAuthenticatedUserDoesNotExist() {
        var handler = new ObtenerMiPerfilHandler(new StubReadPort(Optional.empty()));

        var result = handler.execute(new ObtenerMiPerfilQuery(TENANT_ID, USER_ID));

        assertTrue(result.isFailure());
        assertEquals("SEC_USUARIO_NO_ENCONTRADO", result.fold(value -> null, error -> error.code()));
    }

    @Test
    void requiresItsCollaborators() {
        assertThrows(NullPointerException.class, () -> new ObtenerMiPerfilHandler(null));
        var handler = new ObtenerMiPerfilHandler(new StubReadPort(Optional.empty()));
        assertThrows(NullPointerException.class, () -> handler.execute(null));
    }

    private record StubReadPort(Optional<PerfilResult> profile) implements IamReadPort {

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
            throw new UnsupportedOperationException();
        }

        @Override
        public Optional<PerfilResult> findProfile(UUID tenantId, UUID userId) {
            return profile;
        }
    }
}
