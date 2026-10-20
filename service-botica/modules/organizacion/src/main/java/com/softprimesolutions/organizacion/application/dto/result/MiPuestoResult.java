package com.softprimesolutions.organizacion.application.dto.result;

import java.util.List;

public record MiPuestoResult(List<EstablecimientoPuestoResult> establecimientos) {

    public MiPuestoResult {
        establecimientos = List.copyOf(establecimientos);
    }
}
