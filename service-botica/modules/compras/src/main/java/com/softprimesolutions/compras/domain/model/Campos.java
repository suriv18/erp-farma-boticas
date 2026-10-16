package com.softprimesolutions.compras.domain.model;

import java.math.BigDecimal;

final class Campos {

    static final BigDecimal CANTIDAD_MAXIMA = new BigDecimal("1000000000");
    static final BigDecimal IMPORTE_MAXIMO = new BigDecimal("10000000000");
    static final BigDecimal PORCENTAJE_MAXIMO = new BigDecimal("100");

    private Campos() {
    }

    static String texto(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    static boolean excede(String valor, int maximo) {
        return valor != null && valor.length() > maximo;
    }

    static boolean decimalInvalido(BigDecimal valor, int escala, BigDecimal maximo) {
        return valor.stripTrailingZeros().scale() > escala || valor.compareTo(maximo) > 0;
    }
}
