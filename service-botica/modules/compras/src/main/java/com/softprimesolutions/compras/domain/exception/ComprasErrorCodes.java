package com.softprimesolutions.compras.domain.exception;

public final class ComprasErrorCodes {

    public static final String PROVEEDOR_INVALIDO = "COM_PROVEEDOR_INVALIDO";
    public static final String ORDEN_INVALIDA = "COM_ORDEN_INVALIDA";
    public static final String ORDEN_ESTADO_INVALIDO = "COM_ORDEN_ESTADO_INVALIDO";
    public static final String ORDEN_MOTIVO_INVALIDO = "COM_ORDEN_MOTIVO_INVALIDO";
    public static final String RECEPCION_INVALIDA = "COM_RECEPCION_INVALIDA";
    public static final String RECEPCION_ORDEN_NO_RECEPCIONABLE = "COM_RECEPCION_ORDEN_NO_RECEPCIONABLE";
    public static final String RECEPCION_LINEA_NO_ENCONTRADA = "COM_RECEPCION_LINEA_NO_ENCONTRADA";
    public static final String RECEPCION_EXCEDE_PENDIENTE = "COM_RECEPCION_EXCEDE_PENDIENTE";

    private ComprasErrorCodes() {
    }
}
