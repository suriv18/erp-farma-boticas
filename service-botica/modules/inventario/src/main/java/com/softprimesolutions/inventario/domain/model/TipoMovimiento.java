package com.softprimesolutions.inventario.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum TipoMovimiento {
    AJUSTE_INGRESO(true, "E", true, "99", false),
    AJUSTE_SALIDA(false, "S", true, "99", false),
    INGRESO_COMPRA(true, "E", false, "02", false),
    SALIDA_VENTA(false, "S", false, "01", true);

    private final boolean ingreso;
    private final String naturaleza;
    private final boolean manual;
    private final String tipoOperacionSunat;
    private final boolean exigeLoteVendible;

    TipoMovimiento(
            boolean ingreso, String naturaleza, boolean manual, String tipoOperacionSunat,
            boolean exigeLoteVendible) {
        this.ingreso = ingreso;
        this.naturaleza = naturaleza;
        this.manual = manual;
        this.tipoOperacionSunat = tipoOperacionSunat;
        this.exigeLoteVendible = exigeLoteVendible;
    }

    public static Optional<TipoMovimiento> desde(String valor) {
        return Arrays.stream(values()).filter(tipo -> tipo.name().equals(valor)).findFirst();
    }

    public boolean ingreso() { return ingreso; }
    public String naturaleza() { return naturaleza; }
    public boolean manual() { return manual; }
    public String tipoOperacionSunat() { return tipoOperacionSunat; }
    public boolean exigeLoteVendible() { return exigeLoteVendible; }
}
