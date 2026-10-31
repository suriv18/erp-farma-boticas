package com.softprimesolutions.security.api;

import java.util.UUID;

public interface AutorizacionAmbito {

    boolean permite(UUID tenantId, UUID usuarioId, String permiso, RecursoAmbito recurso);
}
