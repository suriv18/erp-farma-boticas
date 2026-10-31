package com.softprimesolutions.compras.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum EstadoProveedor {
    ACTIVO(true),
    BLOQUEADO(false),
    SUSPENDIDO(false);

    private final boolean admiteCompras;

    EstadoProveedor(boolean admiteCompras) {
        this.admiteCompras = admiteCompras;
    }

    public static Optional<EstadoProveedor> desde(String valor) {
        return Arrays.stream(values()).filter(estado -> estado.name().equals(valor)).findFirst();
    }

    public boolean admiteCompras() { return admiteCompras; }
}
