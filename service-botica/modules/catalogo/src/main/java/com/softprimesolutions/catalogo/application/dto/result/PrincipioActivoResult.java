package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record PrincipioActivoResult(
        UUID id, String codigoFuente, String denominacion, String nombreNormalizado, String fuente, String estado) {
}
