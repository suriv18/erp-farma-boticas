package com.softprimesolutions.catalogo.application.dto.result;

import java.util.UUID;

public record ProductoReguladoResumen(
        UUID id, String denominacion, String condicionVentaCodigo, String estadoRegulatorio) {
}
