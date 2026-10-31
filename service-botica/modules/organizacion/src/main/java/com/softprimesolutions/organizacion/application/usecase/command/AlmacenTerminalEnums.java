package com.softprimesolutions.organizacion.application.usecase.command;

import com.softprimesolutions.organizacion.domain.model.EstadoTerminalPos;
import com.softprimesolutions.organizacion.domain.model.TipoAlmacen;
import java.util.Arrays;

final class AlmacenTerminalEnums {

    private AlmacenTerminalEnums() {
    }

    static TipoAlmacen tipoAlmacen(String value) {
        return Arrays.stream(TipoAlmacen.values())
                .filter(candidate -> candidate.name().equals(value))
                .findFirst()
                .orElse(null);
    }

    static EstadoTerminalPos estadoTerminalPos(String value) {
        return Arrays.stream(EstadoTerminalPos.values())
                .filter(candidate -> candidate.name().equals(value))
                .findFirst()
                .orElse(null);
    }
}
