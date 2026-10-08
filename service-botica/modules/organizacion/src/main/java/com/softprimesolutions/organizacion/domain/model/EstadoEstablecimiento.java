package com.softprimesolutions.organizacion.domain.model;

public enum EstadoEstablecimiento {
    ACTIVO,
    SUSPENDIDO,
    CLAUSURADO,
    REMODELACION;

    public boolean admiteAltasDeHijos() {
        return this == ACTIVO || this == REMODELACION;
    }
}
