package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.RubroComercialId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

/** Rubro comercial (macro-línea): clasifica el surtido en Farma, Bebé, Cosméticos, Retail, etc. */
public final class RubroComercial extends AggregateRoot {

    private static final int CODIGO_MIN_LENGTH = 2;
    private static final int CODIGO_MAX_LENGTH = 50;
    private static final int NOMBRE_MIN_LENGTH = 2;
    private static final int NOMBRE_MAX_LENGTH = 120;
    private static final int DESCRIPCION_MAX_LENGTH = 300;

    private final RubroComercialId id;
    private final TenantId tenantId;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final boolean esFarmaceutico;
    private final int orden;
    private final EstadoRubroComercial estado;

    private RubroComercial(
            RubroComercialId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            boolean esFarmaceutico, int orden, EstadoRubroComercial estado) {
        this.id = id;
        this.tenantId = tenantId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.esFarmaceutico = esFarmaceutico;
        this.orden = orden;
        this.estado = estado;
    }

    public static Result<RubroComercial, ErrorDetail> create(
            RubroComercialId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            boolean esFarmaceutico, int orden) {
        if (id == null) return invalid("id", "La identidad del rubro comercial es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");

        var normalizedCodigo = normalizeSpaces(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() < CODIGO_MIN_LENGTH
                || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código debe tener entre 2 y 50 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < NOMBRE_MIN_LENGTH
                || normalizedNombre.length() > NOMBRE_MAX_LENGTH) {
            return invalid("nombre", "El nombre debe tener entre 2 y 120 caracteres.");
        }

        var normalizedDescripcion = normalizeNullable(descripcion);
        if (!withinLength(normalizedDescripcion, DESCRIPCION_MAX_LENGTH)) {
            return invalid("descripcion", "La descripción no debe exceder 300 caracteres.");
        }

        if (orden < 0) return invalid("orden", "El orden debe ser mayor o igual a 0.");

        return Result.success(new RubroComercial(
                id, tenantId, normalizedCodigo, normalizedNombre, normalizedDescripcion, esFarmaceutico, orden,
                EstadoRubroComercial.ACTIVO));
    }

    public static RubroComercial restore(
            RubroComercialId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            boolean esFarmaceutico, int orden, EstadoRubroComercial estado) {
        return new RubroComercial(id, tenantId, codigo, nombre, descripcion, esFarmaceutico, orden, estado);
    }

    private static Result<RubroComercial, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_RUBRO_COMERCIAL_INVALIDO", message, Map.of("field", field)));
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

    public RubroComercialId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public boolean esFarmaceutico() { return esFarmaceutico; }
    public int orden() { return orden; }
    public EstadoRubroComercial estado() { return estado; }
}
