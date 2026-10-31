package com.softprimesolutions.inventario.domain.exception;

public final class InventarioErrorCodes {

    public static final String LOTE_INVALIDO = "INV_LOTE_INVALIDO";
    public static final String LOTE_VENCIDO = "INV_LOTE_VENCIDO";
    public static final String LOTE_NO_HABILITABLE = "INV_LOTE_NO_HABILITABLE";
    public static final String LOTE_ESTADO_INVALIDO = "INV_LOTE_ESTADO_INVALIDO";
    public static final String LOTE_MOTIVO_INVALIDO = "INV_LOTE_MOTIVO_INVALIDO";
    public static final String CANTIDAD_INVALIDA = "INV_CANTIDAD_INVALIDA";
    public static final String STOCK_INSUFICIENTE = "INV_STOCK_INSUFICIENTE";

    private InventarioErrorCodes() {
    }
}
