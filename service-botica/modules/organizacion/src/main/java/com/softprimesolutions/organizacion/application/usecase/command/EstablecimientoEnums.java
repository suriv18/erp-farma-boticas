package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.domain.model.PerfilOperacion;
import com.softprimesolutions.organizacion.domain.model.TipoEstablecimiento;
import java.util.Arrays;

final class EstablecimientoEnums {

    private EstablecimientoEnums() {
    }

    static TipoEstablecimiento tipoEstablecimiento(String value) {
        return Arrays.stream(TipoEstablecimiento.values())
                .filter(candidate -> candidate.name().equals(value))
                .findFirst()
                .orElse(null);
    }

    static PerfilOperacion perfilOperacion(String value) {
        return Arrays.stream(PerfilOperacion.values())
                .filter(candidate -> candidate.name().equals(value))
                .findFirst()
                .orElse(null);
    }
}
