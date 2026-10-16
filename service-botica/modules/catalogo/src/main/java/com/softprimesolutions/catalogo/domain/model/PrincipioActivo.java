package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.PrincipioActivoId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

/** Sustancia farmacológicamente activa, referenciada por uno o más productos regulados. */
public final class PrincipioActivo extends AggregateRoot {

    private static final int CODIGO_FUENTE_MAX_LENGTH = 80;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 300;
    private static final int NOMBRE_NORMALIZADO_MAX_LENGTH = 300;
    private static final int FUENTE_MAX_LENGTH = 300;

    private final PrincipioActivoId id;
    private final String codigoFuente;
    private final String denominacion;
    private final String nombreNormalizado;
    private final String fuente;
    private final EstadoPrincipioActivo estado;

    private PrincipioActivo(
            PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado,
            String fuente, EstadoPrincipioActivo estado) {
        this.id = id;
        this.codigoFuente = codigoFuente;
        this.denominacion = denominacion;
        this.nombreNormalizado = nombreNormalizado;
        this.fuente = fuente;
        this.estado = estado;
    }

    public static Result<PrincipioActivo, ErrorDetail> create(
            PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado,
            String fuente) {
        if (id == null) return invalid("id", "La identidad del principio activo es obligatoria.");

        var normalizedCodigoFuente = normalizeNullable(codigoFuente);
        if (!withinLength(normalizedCodigoFuente, CODIGO_FUENTE_MAX_LENGTH)) {
            return invalid("codigoFuente", "El código fuente no debe exceder 80 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 300 caracteres.");
        }

        var normalizedNombreNormalizado = normalizeNullable(nombreNormalizado);
        if (!withinLength(normalizedNombreNormalizado, NOMBRE_NORMALIZADO_MAX_LENGTH)) {
            return invalid("nombreNormalizado", "El nombre normalizado no debe exceder 300 caracteres.");
        }

        var normalizedFuente = normalizeNullable(fuente);
        if (!withinLength(normalizedFuente, FUENTE_MAX_LENGTH)) {
            return invalid("fuente", "La fuente no debe exceder 300 caracteres.");
        }

        return Result.success(new PrincipioActivo(
                id, normalizedCodigoFuente, normalizedDenominacion, normalizedNombreNormalizado,
                normalizedFuente, EstadoPrincipioActivo.ACTIVO));
    }

    public static PrincipioActivo restore(
            PrincipioActivoId id, String codigoFuente, String denominacion, String nombreNormalizado,
            String fuente, EstadoPrincipioActivo estado) {
        return new PrincipioActivo(id, codigoFuente, denominacion, nombreNormalizado, fuente, estado);
    }

    private static Result<PrincipioActivo, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_PRINCIPIO_ACTIVO_INVALIDO", message, Map.of("field", field)));
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

    private static boolean withinLength(String value, int maximum) {
        return value == null || value.length() <= maximum;
    }

    public PrincipioActivoId id() { return id; }
    public String codigoFuente() { return codigoFuente; }
    public String denominacion() { return denominacion; }
    public String nombreNormalizado() { return nombreNormalizado; }
    public String fuente() { return fuente; }
    public EstadoPrincipioActivo estado() { return estado; }
}
