package com.softprimesolutions.organizacion.domain.model;

import com.softprimesolutions.organizacion.domain.valueobject.AlmacenId;
import com.softprimesolutions.organizacion.domain.valueobject.EstablecimientoId;
import com.softprimesolutions.organizacion.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;

/** Ubicación de almacenamiento física dentro de un establecimiento. */
public final class Almacen extends AggregateRoot {

    private final AlmacenId id;
    private final TenantId tenantId;
    private final EstablecimientoId establecimientoId;
    private final String codigo;
    private final String nombre;
    private final TipoAlmacen tipo;
    private final boolean permiteLotes;
    private final boolean permiteVencimiento;
    private final boolean permiteVenta;
    private final boolean permiteDespacho;
    private final boolean controlTemperatura;
    private final BigDecimal temperaturaMinC;
    private final BigDecimal temperaturaMaxC;
    private final boolean activo;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Almacen(
            AlmacenId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, boolean activo,
            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.establecimientoId = establecimientoId;
        this.codigo = codigo;
        this.nombre = nombre;
        this.tipo = tipo;
        this.permiteLotes = permiteLotes;
        this.permiteVencimiento = permiteVencimiento;
        this.permiteVenta = permiteVenta;
        this.permiteDespacho = permiteDespacho;
        this.controlTemperatura = controlTemperatura;
        this.temperaturaMinC = temperaturaMinC;
        this.temperaturaMaxC = temperaturaMaxC;
        this.activo = activo;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Almacen, ErrorDetail> create(
            AlmacenId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad del almacén es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (establecimientoId == null) return invalid("establecimientoId", "El establecimiento es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");
        if (tipo == null) return invalid("tipo", "El tipo de almacén es obligatorio.");

        var normalizedCodigo = normalize(codigo);
        if (normalizedCodigo == null || normalizedCodigo.length() > 40) {
            return invalid("codigo", "El código debe tener entre 1 y 40 caracteres.");
        }

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        var temperaturaError = temperaturaError(tipo, controlTemperatura, temperaturaMinC, temperaturaMaxC);
        if (temperaturaError != null) return Result.failure(temperaturaError);

        return Result.success(new Almacen(
                id, tenantId, establecimientoId, normalizedCodigo, normalizedNombre, tipo,
                permiteLotes, permiteVencimiento, permiteVenta, permiteDespacho, controlTemperatura,
                temperaturaMinC, temperaturaMaxC, true, createdAt, null));
    }

    public static Almacen restore(
            AlmacenId id, TenantId tenantId, EstablecimientoId establecimientoId, String codigo,
            String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, boolean activo,
            Instant createdAt, Instant updatedAt) {
        return new Almacen(
                id, tenantId, establecimientoId, codigo, nombre, tipo, permiteLotes, permiteVencimiento,
                permiteVenta, permiteDespacho, controlTemperatura, temperaturaMinC, temperaturaMaxC,
                activo, createdAt, updatedAt);
    }

    public Result<Almacen, ErrorDetail> updateDetails(
            String nombre, TipoAlmacen tipo, boolean permiteLotes, boolean permiteVencimiento,
            boolean permiteVenta, boolean permiteDespacho, boolean controlTemperatura,
            BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC, Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        if (tipo == null) return invalid("tipo", "El tipo de almacén es obligatorio.");

        var normalizedNombre = normalizeSpaces(nombre);
        if (normalizedNombre == null || normalizedNombre.length() < 2 || normalizedNombre.length() > 250) {
            return invalid("nombre", "El nombre debe tener entre 2 y 250 caracteres.");
        }

        var temperaturaError = temperaturaError(tipo, controlTemperatura, temperaturaMinC, temperaturaMaxC);
        if (temperaturaError != null) return Result.failure(temperaturaError);

        return Result.success(new Almacen(
                id, tenantId, establecimientoId, codigo, normalizedNombre, tipo, permiteLotes,
                permiteVencimiento, permiteVenta, permiteDespacho, controlTemperatura,
                temperaturaMinC, temperaturaMaxC, activo, createdAt, updatedAt));
    }

    public Result<Almacen, ErrorDetail> activar(Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new Almacen(
                id, tenantId, establecimientoId, codigo, nombre, tipo, permiteLotes, permiteVencimiento,
                permiteVenta, permiteDespacho, controlTemperatura, temperaturaMinC, temperaturaMaxC,
                true, createdAt, updatedAt));
    }

    public Result<Almacen, ErrorDetail> desactivar(Instant updatedAt) {
        if (updatedAt == null) return invalid("updatedAt", "El instante del cambio es obligatorio.");
        return Result.success(new Almacen(
                id, tenantId, establecimientoId, codigo, nombre, tipo, permiteLotes, permiteVencimiento,
                permiteVenta, permiteDespacho, controlTemperatura, temperaturaMinC, temperaturaMaxC,
                false, createdAt, updatedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(error(field, message));
    }

    private static ErrorDetail error(String field, String message) {
        return new ErrorDetail("ORG_ALMACEN_INVALIDO", message, Map.of("field", field));
    }

    private static ErrorDetail temperaturaError(
            TipoAlmacen tipo, boolean controlTemperatura, BigDecimal temperaturaMinC, BigDecimal temperaturaMaxC) {
        if (tipo == TipoAlmacen.REFRIGERADO && !controlTemperatura) {
            return error("controlTemperatura", "Un almacén refrigerado debe controlar temperatura.");
        }
        if (controlTemperatura && (temperaturaMinC == null || temperaturaMaxC == null)) {
            return error(temperaturaMinC == null ? "temperaturaMinC" : "temperaturaMaxC",
                    "Indica la temperatura mínima y máxima cuando el almacén controla temperatura.");
        }
        if (temperaturaMinC != null && temperaturaMaxC != null && temperaturaMinC.compareTo(temperaturaMaxC) > 0) {
            return error("temperaturaMinC", "La temperatura mínima no puede ser mayor que la máxima.");
        }
        return null;
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim().isEmpty() ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public AlmacenId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public EstablecimientoId establecimientoId() { return establecimientoId; }
    public String codigo() { return codigo; }
    public String nombre() { return nombre; }
    public TipoAlmacen tipo() { return tipo; }
    public boolean permiteLotes() { return permiteLotes; }
    public boolean permiteVencimiento() { return permiteVencimiento; }
    public boolean permiteVenta() { return permiteVenta; }
    public boolean permiteDespacho() { return permiteDespacho; }
    public boolean controlTemperatura() { return controlTemperatura; }
    public BigDecimal temperaturaMinC() { return temperaturaMinC; }
    public BigDecimal temperaturaMaxC() { return temperaturaMaxC; }
    public boolean activo() { return activo; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
