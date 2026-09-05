package com.softprimesolutions.security.domain.model;

import com.softprimesolutions.security.domain.valueobject.ReferenciaIdentidad;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/** Perfil local de autorización vinculado a una identidad externa. */
public final class Usuario extends AggregateRoot {

    private static final int MAX_ISSUER_LENGTH = 500;
    private static final int MAX_SUBJECT_LENGTH = 300;
    private static final int MAX_EMAIL_LENGTH = 254;

    private final UsuarioId id;
    private final TenantId tenantId;
    private final ReferenciaIdentidad identity;
    private final String documentType;
    private final String documentNumber;
    private final String firstNames;
    private final String lastNames;
    private final String username;
    private final String email;
    private final String phone;
    private final String displayName;
    private final boolean credentialChangeRequired;
    private final boolean mfaRequired;
    private final EstadoUsuario status;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Usuario(
            UsuarioId id,
            TenantId tenantId,
            ReferenciaIdentidad identity,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            EstadoUsuario status,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.tenantId = tenantId;
        this.identity = identity;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.username = username;
        this.email = email;
        this.phone = phone;
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
            String provider,
            String issuer,
            String subject,
            String emailClaim,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            Instant createdAt) {
        if (id == null) return invalid("id", "La identidad de usuario es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedProvider = normalize(provider);
        if (!hasLength(normalizedProvider, 1, 100)) {
            return invalid("identityProvider", "El proveedor de identidad es obligatorio y no debe exceder 100 caracteres.");
        }

        var normalizedIssuer = normalizeNullable(issuer);
        if (normalizedIssuer != null && normalizedIssuer.length() > MAX_ISSUER_LENGTH) {
            return invalid("identityIssuer", "El emisor de identidad no debe exceder 500 caracteres.");
        }

        var normalizedSubject = normalize(subject);
        if (!hasLength(normalizedSubject, 1, MAX_SUBJECT_LENGTH)) {
            return invalid("identitySubject", "El subject de identidad es obligatorio y no debe exceder 300 caracteres.");
        }

        var normalizedEmailClaim = normalizeNullable(emailClaim);
        if (normalizedEmailClaim != null && !isValidEmail(normalizedEmailClaim)) {
            return invalid("emailClaim", "El email claim no tiene un formato válido.");
        }
        normalizedEmailClaim = normalizedEmailClaim == null
                ? null : normalizedEmailClaim.toLowerCase(Locale.ROOT);

        var normalizedDocumentType = normalizeNullable(documentType);
        normalizedDocumentType = normalizedDocumentType == null
                ? null
                : normalizedDocumentType.toUpperCase(Locale.ROOT);
        var normalizedDocumentNumber = normalizeNullable(documentNumber);
        if ((normalizedDocumentType == null) != (normalizedDocumentNumber == null)) {
            return invalid("document", "El tipo y número de documento deben informarse juntos.");
        }
        if (normalizedDocumentType != null && (!hasLength(normalizedDocumentType, 1, 20)
                || !hasLength(normalizedDocumentNumber, 1, 30))) {
            return invalid("document", "El documento no cumple las longitudes permitidas.");
        }

        var normalizedFirstNames = normalizeSpaces(firstNames);
        if (normalizedFirstNames != null && !hasLength(normalizedFirstNames, 1, 150)) {
            return invalid("firstNames", "Los nombres no deben exceder 150 caracteres.");
        }
        var normalizedLastNames = normalizeSpaces(lastNames);
        if (normalizedLastNames != null && !hasLength(normalizedLastNames, 1, 180)) {
            return invalid("lastNames", "Los apellidos no deben exceder 180 caracteres.");
        }

        var normalizedUsername = normalizeNullable(username);
        if (normalizedUsername != null && !hasLength(normalizedUsername, 1, 150)) {
            return invalid("username", "El username no debe exceder 150 caracteres.");
        }
        normalizedUsername = normalizedUsername == null ? null : normalizedUsername.toLowerCase(Locale.ROOT);

        var normalizedEmail = normalizeNullable(email);
        if (normalizedEmail != null && !isValidEmail(normalizedEmail)) {
            return invalid("email", "El correo electrónico no tiene un formato válido.");
        }
        normalizedEmail = normalizedEmail == null ? null : normalizedEmail.toLowerCase(Locale.ROOT);

        var normalizedPhone = normalizeNullable(phone);
        if (normalizedPhone != null && normalizedPhone.length() > 40) {
            return invalid("phone", "El teléfono no debe exceder 40 caracteres.");
        }

        var normalizedDisplayName = normalizeSpaces(displayName);
        if (normalizedDisplayName == null) {
            normalizedDisplayName = normalizeSpaces(String.join(" ",
                    normalizedFirstNames == null ? "" : normalizedFirstNames,
                    normalizedLastNames == null ? "" : normalizedLastNames));
        }
        if (normalizedDisplayName != null && normalizedDisplayName.length() > 250) {
            return invalid("displayName", "El nombre para mostrar no debe exceder 250 caracteres.");
        }

        return Result.success(new Usuario(
                id,
                tenantId,
                new ReferenciaIdentidad(normalizedProvider, normalizedSubject, normalizedIssuer, normalizedEmailClaim),
                normalizedDocumentType,
                normalizedDocumentNumber,
                normalizedFirstNames,
                normalizedLastNames,
                normalizedUsername,
                normalizedEmail,
                normalizedPhone,
                normalizedDisplayName,
                credentialChangeRequired,
                mfaRequired,
                EstadoUsuario.ACTIVO,
                createdAt,
                null));
    }

    public static Usuario restore(
            UsuarioId id,
            TenantId tenantId,
            ReferenciaIdentidad identity,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            String displayName,
            boolean credentialChangeRequired,
            boolean mfaRequired,
            EstadoUsuario status,
            Instant createdAt,
            Instant updatedAt) {
        return new Usuario(
                id, tenantId, identity, documentType, documentNumber, firstNames, lastNames,
                username, email, phone, displayName, credentialChangeRequired, mfaRequired,
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

    private static String normalizeNullable(String value) {
        var normalized = normalize(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static boolean hasLength(String value, int minimum, int maximum) {
        return value != null && value.length() >= minimum && value.length() <= maximum;
    }

    private static boolean isValidEmail(String value) {
        if (!hasLength(value, 3, MAX_EMAIL_LENGTH)) return false;
        var at = value.indexOf('@');
        return at > 0 && at == value.lastIndexOf('@') && at < value.length() - 1;
    }

    public UsuarioId id() { return id; }
    public TenantId tenantId() { return tenantId; }
    public ReferenciaIdentidad identity() { return identity; }
    public String documentType() { return documentType; }
    public String documentNumber() { return documentNumber; }
    public String firstNames() { return firstNames; }
    public String lastNames() { return lastNames; }
    public String username() { return username; }
    public String email() { return email; }
    public String phone() { return phone; }
    public String displayName() { return displayName; }
    public boolean credentialChangeRequired() { return credentialChangeRequired; }
    public boolean mfaRequired() { return mfaRequired; }
    public EstadoUsuario status() { return status; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
