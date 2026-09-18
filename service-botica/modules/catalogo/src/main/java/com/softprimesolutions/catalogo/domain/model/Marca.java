package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.MarcaId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

/** Marca comercial de un producto, propia de un tenant. */
public final class Marca extends AggregateRoot {

    private static final int CODIGO_MIN_LENGTH = 2;
    private static final int CODIGO_MAX_LENGTH = 50;
    private static final int NOMBRE_MIN_LENGTH = 2;
    private static final int NOMBRE_MAX_LENGTH = 180;
    private static final int DESCRIPCION_MAX_LENGTH = 500;

    private final MarcaId id;
    private final TenantId tenantId;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final EstadoMarca estado;

    private Marca(
            MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            EstadoMarca estado) {
        this.id = id;
        this.tenantId = tenantId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.estado = estado;
    }

    public static Result<Marca, ErrorDetail> create(
            MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion) {
        if (id == null) return invalid("id", "La identidad de la marca es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");

        var normalizedCodigo = normalizeSpaces(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() < CODIGO_MIN_LENGTH
                || normalizedCodigo.length() > CODIGO_MAX_LENGTH) {
            return invalid("codigo", "El código debe tener entre 2 y 50 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < NOMBRE_MIN_LENGTH
                || normalizedNombre.length() > NOMBRE_MAX_LENGTH) {
            return invalid("nombre", "El nombre debe tener entre 2 y 180 caracteres.");
        }

        var normalizedDescripcion = normalizeNullable(descripcion);
        if (!withinLength(normalizedDescripcion, DESCRIPCION_MAX_LENGTH)) {
            return invalid("descripcion", "La descripción no debe exceder 500 caracteres.");
        }

        return Result.success(new Marca(
                id, tenantId, normalizedCodigo, normalizedNombre, normalizedDescripcion, EstadoMarca.ACTIVO));
    }

    public static Marca restore(
            MarcaId id, TenantId tenantId, String codigo, String nombre, String descripcion,
            EstadoMarca estado) {
        return new Marca(id, tenantId, codigo, nombre, descripcion, estado);
    }

    private static Result<Marca, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_MARCA_INVALIDA", message, Map.of("field", field)));
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

    public MarcaId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public EstadoMarca estado() { return estado; }
}
