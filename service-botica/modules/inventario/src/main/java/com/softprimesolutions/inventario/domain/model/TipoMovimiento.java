package com.softprimesolutions.inventario.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum TipoMovimiento {
    AJUSTE_INGRESO(true, "E", true, "99"),
    AJUSTE_SALIDA(false, "S", true, "99"),
    INGRESO_COMPRA(true, "E", false, "02");

    private final boolean ingreso;
    private final String naturaleza;
    private final boolean manual;
    private final String tipoOperacionSunat;

    TipoMovimiento(boolean ingreso, String naturaleza, boolean manual, String tipoOperacionSunat) {
        this.ingreso = ingreso;
        this.naturaleza = naturaleza;
        this.manual = manual;
        this.tipoOperacionSunat = tipoOperacionSunat;
    }

    public static Optional<TipoMovimiento> desde(String valor) {
        return Arrays.stream(values()).filter(tipo -> tipo.name().equals(valor)).findFirst();
    }

    public boolean ingreso() { return ingreso; }
    public String naturaleza() { return naturaleza; }
    public boolean manual() { return manual; }
    public String tipoOperacionSunat() { return tipoOperacionSunat; }
}
