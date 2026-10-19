package com.softprimesolutions.catalogo.api.dto.response;

import java.util.UUID;

public record PrincipioActivoResponse(
        UUID id, String codigoFuente, String denominacion, String nombreNormalizado, String fuente, String estado) {
}
