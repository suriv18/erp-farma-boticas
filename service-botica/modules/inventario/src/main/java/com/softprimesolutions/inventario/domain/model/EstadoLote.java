package com.softprimesolutions.inventario.domain.model;

public enum EstadoLote {
    HABILITADO(true, true, true),
    CUARENTENA(false, true, true),
    BLOQUEADO(false, false, true),
    INMOVILIZADO_RECALL(false, false, false),
    VENCIDO(false, false, false),
    BAJA_DESTRUIDO(false, false, false);

    private final boolean vendible;
    private final boolean admiteBloqueo;
    private final boolean admiteIngreso;

    EstadoLote(boolean vendible, boolean admiteBloqueo, boolean admiteIngreso) {
        this.vendible = vendible;
        this.admiteBloqueo = admiteBloqueo;
        this.admiteIngreso = admiteIngreso;
    }

    public boolean vendible() { return vendible; }
    public boolean admiteBloqueo() { return admiteBloqueo; }
    public boolean admiteIngreso() { return admiteIngreso; }
}
