package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.model.soporte.EstadoCatalogoSoporte;
import java.time.LocalDate;
import java.util.Objects;

/** Código de barras asociado a un SKU comercial; un SKU puede tener varios, máximo uno principal activo. */
public record CodigoBarraSku(
        String codigoBarra,
        String tipoCodigo,
        boolean esPrincipal,
        LocalDate vigenteDesde,
        LocalDate vigenteHasta,
        EstadoCatalogoSoporte estado) {

    public CodigoBarraSku {
        Objects.requireNonNull(codigoBarra, "codigoBarra es obligatorio");
        Objects.requireNonNull(estado, "estado es obligatorio");
    }
}
