package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/** Catálogo SUNAT 06: código de tipo de documento de identidad. Catálogo global, sin tenant. */
public final class TipoDocumentoIdentidad {

    private static final Pattern CODIGO_PATTERN = Pattern.compile("^[0-9A-Z]$");
    private static final int SIGLA_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;

    private final String codigo;
    private final String sigla;
    private final String denominacion;
    private final Integer max;
    private final Integer min;
    private final EstadoCatalogoSoporte estado;

    private TipoDocumentoIdentidad(
            String codigo, String sigla, String denominacion, Integer max, Integer min,
            EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.sigla = sigla;
        this.denominacion = denominacion;
        this.max = max;
        this.min = min;
        this.estado = estado;
    }

    public static Result<TipoDocumentoIdentidad, ErrorDetail> create(
            String codigo, String sigla, String denominacion, Integer max, Integer min) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || !CODIGO_PATTERN.matcher(normalizedCodigo).matches()) {
            return invalid("codigo", "El código debe ser un único carácter alfanumérico en mayúscula.");
        }

        var normalizedSigla = normalizeNullable(sigla);
        if (normalizedSigla == null || normalizedSigla.length() > SIGLA_MAX_LENGTH) {
            return invalid("sigla", "La sigla es obligatoria y no debe exceder 30 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 200 caracteres.");
        }

        if (max != null && max <= 0) {
            return invalid("max", "El máximo debe ser mayor a 0.");
        }

        if (min != null && min <= 0) {
            return invalid("min", "El mínimo debe ser mayor a 0.");
        }

        if (min != null && max != null && min > max) {
            return invalid("min", "El mínimo no puede ser mayor al máximo.");
        }

        return Result.success(new TipoDocumentoIdentidad(
                normalizedCodigo, normalizedSigla, normalizedDenominacion, max, min, EstadoCatalogoSoporte.ACTIVO));
    }

    public static TipoDocumentoIdentidad restore(
            String codigo, String sigla, String denominacion, Integer max, Integer min,
            EstadoCatalogoSoporte estado) {
        return new TipoDocumentoIdentidad(codigo, sigla, denominacion, max, min, estado);
    }

    private static Result<TipoDocumentoIdentidad, ErrorDetail> invalid(String field, String message) {
        return Result.failure(
                new ErrorDetail("CAT_TIPO_DOCUMENTO_IDENTIDAD_INVALIDO", message, Map.of("field", field)));
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

    public String codigo() { return codigo; }
    public String sigla() { return sigla; }
    public String denominacion() { return denominacion; }
    public Integer max() { return max; }
    public Integer min() { return min; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
