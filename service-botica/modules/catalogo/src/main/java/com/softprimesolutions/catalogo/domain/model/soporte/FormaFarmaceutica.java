package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;

/** Forma en que se presenta un producto farmacéutico (ej. tableta, jarabe, crema). */
public final class FormaFarmaceutica {

    private static final int CODIGO_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;
    private static final int FUENTE_MAX_LENGTH = 300;

    private final String codigo;
    private final String denominacion;
    private final String fuente;
    private final EstadoCatalogoSoporte estado;

    private FormaFarmaceutica(String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.fuente = fuente;
        this.estado = estado;
    }

    public static Result<FormaFarmaceutica, ErrorDetail> create(String codigo, String denominacion, String fuente) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código es obligatorio y no debe exceder 30 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 200 caracteres.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        return Result.success(new FormaFarmaceutica(
                normalizedCodigo, normalizedDenominacion, normalizedFuente, EstadoCatalogoSoporte.ACTIVO));
    }

    public static FormaFarmaceutica restore(
            String codigo, String denominacion, String fuente, EstadoCatalogoSoporte estado) {
        return new FormaFarmaceutica(codigo, denominacion, fuente, estado);
    }

    private static Result<FormaFarmaceutica, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_FORMA_FARMACEUTICA_INVALIDA", message, Map.of("field", field)));
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
    public String fuente() { return fuente; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
