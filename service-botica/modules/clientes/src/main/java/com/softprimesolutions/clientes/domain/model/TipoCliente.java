package com.softprimesolutions.clientes.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum TipoCliente {
    NATURAL,
    JURIDICO,
    SIN_IDENTIFICAR;

    public static Optional<TipoCliente> parse(String value) {
        return Arrays.stream(values()).filter(tipo -> tipo.name().equals(value)).findFirst();
    }
}
