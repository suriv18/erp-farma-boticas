package com.softprimesolutions.inventario.domain.model;

import java.math.BigDecimal;

public record AplicacionMovimiento(
        PosicionInventario posicion, BigDecimal stockAnterior, BigDecimal stockPosterior) {
}
