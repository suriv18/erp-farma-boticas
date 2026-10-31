package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import java.math.BigDecimal;
import java.util.Objects;

/** Relación entre un producto regulado y uno de sus principios activos, con su concentración. */
public record PrincipioActivoAsociado(
        PrincipioActivoId principioActivoId,
        String concentracionTexto,
        BigDecimal cantidad,
        String unidadMedidaCodigo,
        boolean esPrincipal,
        short orden) {

    public PrincipioActivoAsociado {
        Objects.requireNonNull(principioActivoId, "principioActivoId es obligatorio");
    }
}
