package com.softprimesolutions.clientes.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum EstadoCliente {
    ACTIVO,
    BLOQUEADO;

    public static Optional<EstadoCliente> parse(String value) {
        return Arrays.stream(values()).filter(estado -> estado.name().equals(value)).findFirst();
    }
}
