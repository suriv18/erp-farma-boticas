package com.softprimesolutions.security.domain.model;

import com.softprimesolutions.security.domain.valueobject.IdentidadId;
import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/** Persona identificable de forma unica en todo el sistema, independiente de su relacion con un tenant. */
public final class Identidad extends AggregateRoot {

    private static final int MAX_EMAIL_LENGTH = 254;

    private final IdentidadId id;
    private final String documentType;
    private final String documentNumber;
    private final String firstNames;
    private final String lastNames;
    private final String username;
    private final String email;
    private final String phone;
    private final Instant createdAt;
    private final Instant updatedAt;

    private Identidad(
            IdentidadId id,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.documentType = documentType;
        this.documentNumber = documentNumber;
        this.firstNames = firstNames;
        this.lastNames = lastNames;
        this.username = username;
        this.email = email;
        this.phone = phone;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static Result<Identidad, ErrorDetail> register(
            IdentidadId id,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            Instant createdAt) {
        if (id == null) return invalid("id", "La identidad es obligatoria.");
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");

        var normalizedDocumentType = normalizeNullable(documentType);
        normalizedDocumentType = normalizedDocumentType == null
                ? null : normalizedDocumentType.toUpperCase(Locale.ROOT);
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
        if (!isValidEmail(normalizedEmail)) {
            return invalid("email", "El correo electrónico no tiene un formato válido.");
        }
        normalizedEmail = normalizedEmail.toLowerCase(Locale.ROOT);

        var normalizedPhone = normalizeNullable(phone);
        if (normalizedPhone != null && normalizedPhone.length() > 40) {
            return invalid("phone", "El teléfono no debe exceder 40 caracteres.");
        }

        return Result.success(new Identidad(
                id, normalizedDocumentType, normalizedDocumentNumber, normalizedFirstNames,
                normalizedLastNames, normalizedUsername, normalizedEmail, normalizedPhone,
                createdAt, null));
    }

    public static Identidad restore(
            IdentidadId id,
            String documentType,
            String documentNumber,
            String firstNames,
            String lastNames,
            String username,
            String email,
            String phone,
            Instant createdAt,
            Instant updatedAt) {
        return new Identidad(
                id, documentType, documentNumber, firstNames, lastNames, username, email, phone,
                createdAt, updatedAt);
    }

    private static Result<Identidad, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("SEC_IDENTIDAD_INVALIDA", message, Map.of("field", field)));
    }

    private static String normalizeSpaces(String value) {
        var normalized = normalize(value);
        return normalized == null ? null : normalized.replaceAll("\\s+", " ");
    }

    private static String normalize(String value) {
        return value == null ? null : value.trim();
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

    public IdentidadId id() { return id; }
    public String documentType() { return documentType; }
    public String documentNumber() { return documentNumber; }
    public String firstNames() { return firstNames; }
    public String lastNames() { return lastNames; }
    public String username() { return username; }
    public String email() { return email; }
    public String phone() { return phone; }
    public Instant createdAt() { return createdAt; }
    public Instant updatedAt() { return updatedAt; }
}
