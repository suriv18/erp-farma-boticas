package com.softprimesolutions.catalogo.domain.model.soporte;

import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

/** Condición regulatoria de venta de un producto (ej. sin receta, con receta, receta retenida). */
public final class CondicionVenta {

    private static final int CODIGO_MAX_LENGTH = 30;
    private static final int DENOMINACION_MIN_LENGTH = 2;
    private static final int DENOMINACION_MAX_LENGTH = 200;
    private static final int FUENTE_MAX_LENGTH = 300;
    private static final int VERSION_FUENTE_MAX_LENGTH = 100;

    private final String codigo;
    private final String denominacion;
    private final boolean requiereReceta;
    private final boolean requiereRetencion;
    private final String fuente;
    private final String versionFuente;
    private final LocalDate vigenteDesde;
    private final LocalDate vigenteHasta;
    private final EstadoCatalogoSoporte estado;

    private CondicionVenta(
            String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
            String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta,
            EstadoCatalogoSoporte estado) {
        this.codigo = codigo;
        this.denominacion = denominacion;
        this.requiereReceta = requiereReceta;
        this.requiereRetencion = requiereRetencion;
        this.fuente = fuente;
        this.versionFuente = versionFuente;
        this.vigenteDesde = vigenteDesde;
        this.vigenteHasta = vigenteHasta;
        this.estado = estado;
    }

    public static Result<CondicionVenta, ErrorDetail> create(
            String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
            String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta) {
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

        var normalizedVersionFuente = normalizeNullable(versionFuente);
        if (!withinLength(normalizedVersionFuente, VERSION_FUENTE_MAX_LENGTH)) {
            return invalid("versionFuente", "La versión de fuente no debe exceder 100 caracteres.");
        }

        if (vigenteDesde != null && vigenteHasta != null && vigenteHasta.isBefore(vigenteDesde)) {
            return invalid("vigenteHasta", "La vigencia hasta no puede ser anterior a la vigencia desde.");
        }

        return Result.success(new CondicionVenta(
                normalizedCodigo, normalizedDenominacion, requiereReceta, requiereRetencion,
                normalizedFuente, normalizedVersionFuente, vigenteDesde, vigenteHasta,
                EstadoCatalogoSoporte.ACTIVO));
    }

    public static CondicionVenta restore(
            String codigo, String denominacion, boolean requiereReceta, boolean requiereRetencion,
            String fuente, String versionFuente, LocalDate vigenteDesde, LocalDate vigenteHasta,
            EstadoCatalogoSoporte estado) {
        return new CondicionVenta(
                codigo, denominacion, requiereReceta, requiereRetencion, fuente, versionFuente,
                vigenteDesde, vigenteHasta, estado);
    }

    private static Result<CondicionVenta, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_CONDICION_VENTA_INVALIDA", message, Map.of("field", field)));
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
    public boolean requiereReceta() { return requiereReceta; }
    public boolean requiereRetencion() { return requiereRetencion; }
    public String fuente() { return fuente; }
    public String versionFuente() { return versionFuente; }
    public LocalDate vigenteDesde() { return vigenteDesde; }
    public LocalDate vigenteHasta() { return vigenteHasta; }
    public EstadoCatalogoSoporte estado() { return estado; }
}
