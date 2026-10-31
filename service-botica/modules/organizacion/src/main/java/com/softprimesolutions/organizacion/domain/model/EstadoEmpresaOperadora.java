package com.softprimesolutions.organizacion.domain.model;

public enum EstadoEmpresaOperadora {
    ACTIVO,
    SUSPENDIDO,
    BLOQUEADO;

    public boolean admiteAltasDeHijos() {
        return this == ACTIVO;
    }
}
