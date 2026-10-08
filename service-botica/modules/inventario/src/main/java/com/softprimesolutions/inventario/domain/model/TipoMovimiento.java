package com.softprimesolutions.inventario.domain.model;

import java.util.Arrays;
import java.util.Optional;

public enum TipoMovimiento {
    AJUSTE_INGRESO(true, "E", true, "99", false, false),
    AJUSTE_SALIDA(false, "S", true, "99", false, false),
    INGRESO_COMPRA(true, "E", false, "02", false, false),
    SALIDA_VENTA(false, "S", false, "01", true, false),
    ANULACION_VENTA(true, "E", false, "05", false, true);

    private final boolean ingreso;
    private final String naturaleza;
    private final boolean manual;
    private final String tipoOperacionSunat;
    private final boolean exigeLoteVendible;
    private final boolean ignoraEstadoLoteEnIngreso;

    TipoMovimiento(
            boolean ingreso, String naturaleza, boolean manual, String tipoOperacionSunat,
            boolean exigeLoteVendible, boolean ignoraEstadoLoteEnIngreso) {
        this.ingreso = ingreso;
        this.naturaleza = naturaleza;
        this.manual = manual;
        this.tipoOperacionSunat = tipoOperacionSunat;
        this.exigeLoteVendible = exigeLoteVendible;
        this.ignoraEstadoLoteEnIngreso = ignoraEstadoLoteEnIngreso;
    }

    public static Optional<TipoMovimiento> desde(String valor) {
        return Arrays.stream(values()).filter(tipo -> tipo.name().equals(valor)).findFirst();
    }

    public boolean ingreso() { return ingreso; }
    public String naturaleza() { return naturaleza; }
    public boolean manual() { return manual; }
    public String tipoOperacionSunat() { return tipoOperacionSunat; }
    public boolean exigeLoteVendible() { return exigeLoteVendible; }
    public boolean ignoraEstadoLoteEnIngreso() { return ignoraEstadoLoteEnIngreso; }
}
