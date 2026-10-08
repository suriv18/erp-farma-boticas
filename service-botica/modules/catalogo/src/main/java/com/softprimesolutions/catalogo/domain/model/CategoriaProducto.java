package com.softprimesolutions.catalogo.domain.model;

import com.softprimesolutions.catalogo.domain.valueobject.CategoriaProductoId;
import com.softprimesolutions.catalogo.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.util.Map;

/** Categoría jerárquica de producto, propia de un tenant. */
public final class CategoriaProducto extends AggregateRoot {

    private static final int CODIGO_MIN_LENGTH = 2;
    private static final int CODIGO_MAX_LENGTH = 50;
    private static final int NOMBRE_MIN_LENGTH = 2;
    private static final int NOMBRE_MAX_LENGTH = 180;
    private static final int DESCRIPCION_MAX_LENGTH = 500;

    private final CategoriaProductoId id;
    private final TenantId tenantId;
    private final CategoriaProductoId categoriaPadreId;
    private final String codigo;
    private final String nombre;
    private final String descripcion;
    private final int nivel;
    private final int orden;
    private final EstadoCategoriaProducto estado;

    private CategoriaProducto(
            CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo,
            String nombre, String descripcion, int nivel, int orden, EstadoCategoriaProducto estado) {
        this.id = id;
        this.tenantId = tenantId;
        this.categoriaPadreId = categoriaPadreId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.descripcion = descripcion;
        this.nivel = nivel;
        this.orden = orden;
        this.estado = estado;
    }

    public static Result<CategoriaProducto, ErrorDetail> create(
            CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo,
            String nombre, String descripcion, int nivel, int orden) {
        if (id == null) return invalid("id", "La identidad de la categoría es obligatoria.");
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

        if (nivel < 1) return invalid("nivel", "El nivel debe ser mayor o igual a 1.");
        if (orden < 0) return invalid("orden", "El orden debe ser mayor o igual a 0.");

        return Result.success(new CategoriaProducto(
                id, tenantId, categoriaPadreId, normalizedCodigo, normalizedNombre, normalizedDescripcion,
                nivel, orden, EstadoCategoriaProducto.ACTIVO));
    }

    public static CategoriaProducto restore(
            CategoriaProductoId id, TenantId tenantId, CategoriaProductoId categoriaPadreId, String codigo,
            String nombre, String descripcion, int nivel, int orden, EstadoCategoriaProducto estado) {
        return new CategoriaProducto(
                id, tenantId, categoriaPadreId, codigo, nombre, descripcion, nivel, orden, estado);
    }

    private static Result<CategoriaProducto, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("CAT_CATEGORIA_PRODUCTO_INVALIDA", message, Map.of("field", field)));
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

    public CategoriaProductoId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public CategoriaProductoId categoriaPadreId() { return categoriaPadreId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public String descripcion() { return descripcion; }
    public int nivel() { return nivel; }
    public int orden() { return orden; }
    public EstadoCategoriaProducto estado() { return estado; }
}
