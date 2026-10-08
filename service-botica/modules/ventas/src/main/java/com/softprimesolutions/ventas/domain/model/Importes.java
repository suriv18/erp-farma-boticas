package com.softprimesolutions.ventas.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class Importes {

    private static final BigDecimal MAXIMO = new BigDecimal("1000000000");
    private static final int ESCALA = 2;
    private static final int ESCALA_UNITARIA = 4;

    private Importes() {
    }

    public static boolean montoValido(BigDecimal valor) {
        return enRango(valor, ESCALA) && valor.signum() >= 0;
    }

    public static boolean precioValido(BigDecimal valor) {
        return enRango(valor, ESCALA_UNITARIA) && valor.signum() >= 0;
    }

    public static boolean cantidadValida(BigDecimal valor) {
        return enRango(valor, ESCALA_UNITARIA) && valor.signum() > 0;
    }

    public static BigDecimal redondear(BigDecimal valor) {
        return valor.setScale(ESCALA, RoundingMode.HALF_UP);
    }

    private static boolean enRango(BigDecimal valor, int escalaMaxima) {
        return valor != null
                && valor.compareTo(MAXIMO) <= 0
                && valor.stripTrailingZeros().scale() <= escalaMaxima;
    }
}
