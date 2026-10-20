package com.softprimesolutions.security.api;

import java.util.UUID;

public interface AlcanceUsuario {

    AlcanceResultado alcanceDe(UUID tenantId, UUID usuarioId);
}
