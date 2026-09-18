package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Locale;
import java.util.Map;

/** Clasificación de sustancias/productos controlados (ej. listas de estupefacientes/psicotrópicos). */
public final class ClasificacionControlada {

    private static final int CODIGO_MAX_LENGTH = 40;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;
    private static final int NORMA_FUENTE_MAX_LENGTH = 300;

    private final String codigo;
    private final String denominacion;
    private final String normaFuente;
    private final boolean requiereRecetaEspecial;
    private final boolean retieneReceta;
    private final Integer vigenciaRecetaDias;
    private final EstadoCatalogoSoporte estado;

    private ClasificacionControlada(
            String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
            boolean retieneReceta, Integer vigenciaRecetaDias, EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.normaFuente = normaFuente;
        this.requiereRecetaEspecial = requiereRecetaEspecial;
        this.retieneReceta = retieneReceta;
        this.vigenciaRecetaDias = vigenciaRecetaDias;
        this.estado = estado;
    }

    public static Result<ClasificacionControlada, ErrorDetail> create(
            String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
            boolean retieneReceta, Integer vigenciaRecetaDias) {
        var normalizedCodigo = normalizeUpper(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código es obligatorio y no debe exceder 40 caracteres.");
        }

        var normalizedDenominacion = normalizeSpaces(denominacion);
        if (normalizedDenominacion == null || normalizedDenominacion.length() < DENOMINACION_MIN_LENGTH
                || normalizedDenominacion.length() > DENOMINACION_MAX_LENGTH) {
            return invalid("denominacion", "La denominación debe tener entre 2 y 200 caracteres.");
        }

        var normalizedNormaFuente = normalizeNullable(normaFuente);
        if (!withinLength(normalizedNormaFuente, NORMA_FUENTE_MAX_LENGTH)) {
            return invalid("normaFuente", "La norma fuente no debe exceder 300 caracteres.");
        }

        if (vigenciaRecetaDias != null && vigenciaRecetaDias <= 0) {
            return invalid("vigenciaRecetaDias", "La vigencia de receta en días debe ser mayor que cero.");
        }

        return Result.success(new ClasificacionControlada(
                normalizedCodigo, normalizedDenominacion, normalizedNormaFuente, requiereRecetaEspecial,
                retieneReceta, vigenciaRecetaDias, EstadoCatalogoSoporte.ACTIVO));
    }

    public static ClasificacionControlada restore(
            String codigo, String denominacion, String normaFuente, boolean requiereRecetaEspecial,
            boolean retieneReceta, Integer vigenciaRecetaDias, EstadoCatalogoSoporte estado) {
        return new ClasificacionControlada(
                codigo, denominacion, normaFuente, requiereRecetaEspecial, retieneReceta, vigenciaRecetaDias,
                estado);
    }

    private static Result<ClasificacionControlada, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail(
                "CAT_CLASIFICACION_CONTROLADA_INVALIDA", message, Map.of("field", field)));
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
    public String normaFuente() { return normaFuente; }
    public boolean requiereRecetaEspecial() { return requiereRecetaEspecial; }
    public boolean retieneReceta() { return retieneReceta; }
    public Integer vigenciaRecetaDias() { return vigenciaRecetaDias; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
