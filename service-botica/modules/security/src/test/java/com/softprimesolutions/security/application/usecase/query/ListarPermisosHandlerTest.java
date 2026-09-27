package com.softprimesolutions.security.application.usecase.query;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.softprimesolutions.security.application.dto.query.ListarPermisosQuery;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.application.port.out.IamReadPort;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListarPermisosHandlerTest {

    @Test
    void returnsPagedResultFromReadPort() {
        var expected = new PaginaResult<>(
                List.of(new PermisoResult(
                        "SEGURIDAD", "Seguridad", "seguridad.usuarios.consultar", "USUARIO", "CONSULTAR",
                        "Consultar usuarios", "Permite consultar usuarios.", false, "ACTIVO")),
                0, 20, 1L);
        var port = new StubReadPort(expected);
        var handler = new ListarPermisosHandler(port);

        var result = handler.execute(new ListarPermisosQuery("usuarios", 0, 20));

        assertTrue(result.isSuccess());
        assertEquals(expected, result.getOrElse(error -> null));
        assertEquals("usuarios", port.receivedSearch);
        assertEquals(0, port.receivedPage);
        assertEquals(20, port.receivedSize);
    }

    @Test
    void rejectsInvalidPagination() {
        var handler = new ListarPermisosHandler(new StubReadPort(null));

        var result = handler.execute(new ListarPermisosQuery(null, -1, 20));

        assertTrue(result.isFailure());
        assertEquals("SEC_PAGINACION_INVALIDA", result.fold(value -> null, error -> error.code()));
    }

    private static final class StubReadPort implements IamReadPort {

        private final PaginaResult<PermisoResult> toReturn;
        private String receivedSearch;
        private int receivedPage;
        private int receivedSize;

        private StubReadPort(PaginaResult<PermisoResult> toReturn) {
            this.toReturn = toReturn;
        }

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
            this.receivedSearch = search;
            this.receivedPage = page;
            this.receivedSize = size;
            return toReturn;
        }
    }
}
