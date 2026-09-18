package com.softprimesolutions.catalogo.api.dto.response;

import java.util.UUID;

public record ProductoReguladoResumenResponse(
        UUID id, String denominacion, String condicionVentaCodigo, String estadoRegulatorio) {
}
