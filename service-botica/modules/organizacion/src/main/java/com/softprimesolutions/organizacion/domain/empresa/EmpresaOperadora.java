package com.softprimesolutions.organizacion.domain.empresa;

import com.softprimesolutions.shared.kernel.domain.AggregateRoot;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;

/** Agregado raíz que representa a la entidad legal/corporativa que opera la cadena. */
public final class EmpresaOperadora extends AggregateRoot {

    private static final int IDENTIFIER_TYPE_MIN_LENGTH = 2;
    private static final int IDENTIFIER_TYPE_MAX_LENGTH = 20;
    private static final int IDENTIFIER_VALUE_MIN_LENGTH = 3;
    private static final int IDENTIFIER_VALUE_MAX_LENGTH = 40;
    private static final int LEGAL_NAME_MIN_LENGTH = 2;
    private static final int LEGAL_NAME_MAX_LENGTH = 200;
    private static final int TRADE_NAME_MAX_LENGTH = 160;

    private final EmpresaOperadoraId id;
    private final String identifierType;
    private final String identifierValue;
    private final String legalName;
    private final String tradeName;
    private final Instant registeredAt;
    private final EstadoEmpresaOperadora status;

    private EmpresaOperadora(
            EmpresaOperadoraId id,
            String identifierType,
            String identifierValue,
            String legalName,
            String tradeName,
            Instant registeredAt) {
        this.id = id;
        this.identifierType = identifierType;
        this.identifierValue = identifierValue;
        this.legalName = legalName;
        this.tradeName = tradeName;
        this.registeredAt = registeredAt;
        this.status = EstadoEmpresaOperadora.ACTIVE;
    }

    public static Result<EmpresaOperadora, ErrorDetail> register(
            EmpresaOperadoraId id,
            String identifierType,
            String identifierValue,
            String legalName,
            String tradeName,
            Instant registeredAt) {
        if (id == null) return invalid("id", "La identidad de empresa es obligatoria.");
        if (registeredAt == null) {
            return invalid("registeredAt", "El instante de registro es obligatorio.");
        }

        var normalizedIdentifierType = normalizeUpper(identifierType);
        if (!hasLengthBetween(
                normalizedIdentifierType,
                IDENTIFIER_TYPE_MIN_LENGTH,
                IDENTIFIER_TYPE_MAX_LENGTH)) {
            return invalid("identifierType", "El tipo de identificador debe tener entre 2 y 20 caracteres.");
        }

        var normalizedIdentifierValue = normalizeUpper(identifierValue);
        if (!hasLengthBetween(
                normalizedIdentifierValue,
                IDENTIFIER_VALUE_MIN_LENGTH,
                IDENTIFIER_VALUE_MAX_LENGTH)) {
            return invalid("identifierValue", "El identificador debe tener entre 3 y 40 caracteres.");
        }

        var normalizedLegalName = normalizeText(legalName);
        if (!hasLengthBetween(normalizedLegalName, LEGAL_NAME_MIN_LENGTH, LEGAL_NAME_MAX_LENGTH)) {
            return invalid("legalName", "La razón social debe tener entre 2 y 200 caracteres.");
        }

        var normalizedTradeName = normalizeNullableText(tradeName);
        if (normalizedTradeName != null && normalizedTradeName.length() > TRADE_NAME_MAX_LENGTH) {
            return invalid("tradeName", "El nombre comercial no debe exceder 160 caracteres.");
        }

        return Result.success(new EmpresaOperadora(
                id,
                normalizedIdentifierType,
                normalizedIdentifierValue,
                normalizedLegalName,
                normalizedTradeName,
                registeredAt));
    }

    private static Result<EmpresaOperadora, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail(
                "ORG_EMPRESA_INVALIDA",
                message,
                Map.of("field", field)));
    }

    private static String normalizeUpper(String value) {
        var normalized = normalizeText(value);
        return normalized == null ? null : normalized.toUpperCase(Locale.ROOT);
    }

    private static String normalizeText(String value) {
        if (value == null) return null;
        return value.trim().replaceAll("\\s+", " ");
    }

    private static String normalizeNullableText(String value) {
        var normalized = normalizeText(value);
        return normalized == null || normalized.isEmpty() ? null : normalized;
    }

    private static boolean hasLengthBetween(String value, int minimum, int maximum) {
        return value != null && value.length() >= minimum && value.length() <= maximum;
    }

    public EmpresaOperadoraId id() {
        return id;
    }

    public String identifierType() {
        return identifierType;
    }

    public String identifierValue() {
        return identifierValue;
    }

    public String legalName() {
        return legalName;
    }

    public String tradeName() {
        return tradeName;
    }

    public Instant registeredAt() {
        return registeredAt;
    }

    public EstadoEmpresaOperadora status() {
        return status;
    }
}
