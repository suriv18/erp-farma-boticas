package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;

/** Unidad de medida usada para cantidades y concentraciones (ej. mg, ml, comprimidos). */
public final class UnidadMedida {

    private static final int CODIGO_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 150;
    private static final int SIMBOLO_MAX_LENGTH = 30;
    private static final int FUENTE_MAX_LENGTH = 300;

    private final String codigo;
    private final String denominacion;
    private final String simbolo;
    private final boolean permiteDecimal;
    private final String fuente;
    private final EstadoCatalogoSoporte estado;

    private UnidadMedida(
            String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente,
            EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.simbolo = simbolo;
        this.permiteDecimal = permiteDecimal;
        this.fuente = fuente;
        this.estado = estado;
    }

    public static Result<UnidadMedida, ErrorDetail> create(
            String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código es obligatorio y no debe exceder 30 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 150 caracteres.");
        }

        var normalizedSimbolo = normalizeNullable(simbolo);
        if (!withinLength(normalizedSimbolo, SIMBOLO_MAX_LENGTH)) {
            return invalid("simbolo", "El símbolo no debe exceder 30 caracteres.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        return Result.success(new UnidadMedida(
                normalizedCodigo, normalizedDenominacion, normalizedSimbolo, permiteDecimal, normalizedFuente,
                EstadoCatalogoSoporte.ACTIVO));
    }

    public static UnidadMedida restore(
            String codigo, String denominacion, String simbolo, boolean permiteDecimal, String fuente,
            EstadoCatalogoSoporte estado) {
        return new UnidadMedida(codigo, denominacion, simbolo, permiteDecimal, fuente, estado);
    }

    private static Result<UnidadMedida, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_UNIDAD_MEDIDA_INVALIDA", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    private static String normalizeNullable(String value) {
        var normalized = normalizeSpaces(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static String normalizeUpper(String value) {
        var normalized = normalizeNullable(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public String codigo() { return codigo; }
    public String denominacion() { return denominacion; }
    public String simbolo() { return simbolo; }
    public boolean permiteDecimal() { return permiteDecimal; }
    public String fuente() { return fuente; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
