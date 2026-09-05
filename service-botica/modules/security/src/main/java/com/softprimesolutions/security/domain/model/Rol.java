package com.softprimesolutions.security.domain.model;

import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/** Rol funcional compuesto por permisos estables del catálogo. */
public final class Rol extends AggregateRoot {

    private static final Pattern CODE_PATTERN = Pattern.compile("[A-Z][A-Z0-9_]{2,79}");
    private static final Pattern PERMISSION_PATTERN = Pattern.compile(
            "[a-z][a-z0-9_-]*(\\.[a-z][a-z0-9_-]*){2,}");

    private final RolId id;
    private final TenantId tenantId;
    private final String code;
    private final String name;
    private final String description;
    private final TipoRol roleType;
    private final boolean systemRole;
    private final Set<String> permissionCodes;
    private final EstadoRol status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Rol(
            RolId id,
            TenantId tenantId,
            String code,
            String name,
            String description,
            TipoRol roleType,
            boolean systemRole,
            Set<String> permissionCodes,
            EstadoRol status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.code = code;
        this.name = name;
        this.description = description;
        this.roleType = roleType;
        this.systemRole = systemRole;
        this.permissionCodes = Set.copyOf(permissionCodes);
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Rol, ErrorDetail> create(
            RolId id, TenantId tenantId, String code, String name, String description,
            String roleType, boolean systemRole, Instant createdAt) {
        if (id == null) return invalid("id", "La identidad de rol es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedCode = normalize(code);
        normalizedCode = normalizedCode == null ? null : normalizedCode.toUpperCase(Locale.ROOT);
        if (normalizedCode == null || !CODE_PATTERN.matcher(normalizedCode).matches()) {
            return invalid("code", "El código debe usar entre 3 y 80 caracteres A-Z, 0-9 o guion bajo.");
        }

        var normalizedName = normalizeSpaces(name);
        if (normalizedName == null || normalizedName.length() < 2 || normalizedName.length() > 150) {
            return invalid("name", "El nombre debe tener entre 2 y 150 caracteres.");
        }

        var normalizedDescription = normalizeSpaces(description);
        if (normalizedDescription != null && normalizedDescription.length() > 500) {
            return invalid("description", "La descripción no debe exceder 500 caracteres.");
        }

        final TipoRol normalizedRoleType;
        try {
            normalizedRoleType = TipoRol.valueOf(roleType == null ? "" : roleType.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return invalid("roleType", "El tipo de rol no es válido.");
        }

        return Result.success(new Rol(
                id,
                tenantId,
                normalizedCode,
                normalizedName,
                normalizedDescription,
                normalizedRoleType,
                systemRole,
                Set.of(),
                EstadoRol.ACTIVO,
                createdAt,
                null));
    }

    public static Rol restore(
            RolId id,
            TenantId tenantId,
            String code,
            String name,
            String description,
            TipoRol roleType,
            boolean systemRole,
            Set<String> permissionCodes,
            EstadoRol status,
            Instant createdAt,
            Instant updatedAt) {
        return new Rol(id, tenantId, code, name, description, roleType, systemRole,
                permissionCodes, status, createdAt, updatedAt);
    }

    public Result<Rol, ErrorDetail> replacePermissions(Set<String> codes, Instant changedAt) {
        if (codes == null) return invalid("permissionCodes", "La colección de permisos es obligatoria.");
        if (changedAt == null) return invalid("changedAt", "El instante del cambio es obligatorio.");
        var normalized = new java.util.HashSet<String>();
        for (var codeValue : codes) {
            var normalizedCode = normalize(codeValue);
            normalizedCode = normalizedCode == null ? null : normalizedCode.toLowerCase(Locale.ROOT);
            if (normalizedCode == null || !PERMISSION_PATTERN.matcher(normalizedCode).matches()) {
                return invalid("permissionCodes", "Existe un código de permiso con formato inválido.");
            }
            normalized.add(normalizedCode);
        }
        return Result.success(new Rol(
                id, tenantId, code, name, description, roleType, systemRole,
                normalized, status, createdAt, changedAt));
    }

    private static <T> Result<T, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("SEC_ROL_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null || normalized.isEmpty() ? null : normalized.replaceAll("\\s+", " ");
    }

    public RolId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public String code() { return code; }
    public String name() { return name; }
    public String description() { return description; }
    public TipoRol roleType() { return roleType; }
    public boolean systemRole() { return systemRole; }
    public Set<String> permissionCodes() { return permissionCodes; }
    public EstadoRol status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
