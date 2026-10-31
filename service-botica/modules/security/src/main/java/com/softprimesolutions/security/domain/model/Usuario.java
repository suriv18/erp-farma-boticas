package com.softprimesolutions.security.domain.model;

import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Map;

/** Relacion (membership) entre una Identidad y un tenant: rol, estado y credenciales locales. */
public final class Usuario extends AggregateRoot {

    private final UsuarioId id;
    private final TenantId tenantId;
    private final IdentidadId identidadId;
    private final String displayName;
    private final boolean credentialChangeRequired;
    private final boolean mfaRequired;
    private final EstadoUsuario status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Usuario(
            UsuarioId id,
            TenantId tenantId,
            IdentidadId identidadId,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            EstadoUsuario status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.identidadId = identidadId;
        this.displayName = displayName;
        this.credentialChangeRequired = credentialChangeRequired;
        this.mfaRequired = mfaRequired;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Usuario, ErrorDetail> register(
            UsuarioId id,
            TenantId tenantId,
            IdentidadId identidadId,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            Instant createdAt) {
        if (id == null) return invalid("id", "La identidad de membership es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (identidadId == null) return invalid("identidadId", "La identidad es obligatoria.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedDisplayName = normalizeSpaces(displayName);
        if (normalizedDisplayName != null && normalizedDisplayName.length() > 250) {
            return invalid("displayName", "El nombre para mostrar no debe exceder 250 caracteres.");
        }

        return Result.success(new Usuario(
                id, tenantId, identidadId, normalizedDisplayName, credentialChangeRequired,
                mfaRequired, EstadoUsuario.ACTIVO, createdAt, null));
    }

    public static Usuario restore(
            UsuarioId id,
            TenantId tenantId,
            IdentidadId identidadId,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            EstadoUsuario status,
            Instant createdAt,
            Instant updatedAt) {
        return new Usuario(
                id, tenantId, identidadId, displayName, credentialChangeRequired, mfaRequired,
                status, createdAt, updatedAt);
    }

    private static Result<Usuario, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("SEC_USUARIO_INVALIDO", message, Map.of("field", field)));
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    public UsuarioId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public IdentidadId identidadId() { return identidadId; }
    public String displayName() { return displayName; }
    public boolean credentialChangeRequired() { return credentialChangeRequired; }
    public boolean mfaRequired() { return mfaRequired; }
    public EstadoUsuario status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
