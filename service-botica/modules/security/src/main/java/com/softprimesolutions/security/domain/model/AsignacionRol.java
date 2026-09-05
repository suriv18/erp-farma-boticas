package com.softprimesolutions.security.domain.model;

import com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional;
import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.domain.valueobject.TipoAmbito;
import com.softprimesolutions.security.domain.valueobject.UsuarioId;
import com.softprimesolutions.shared.kernel.error.ErrorDetail;
import com.softprimesolutions.shared.kernel.result.Result;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/** Asigna un rol a un usuario dentro de un ámbito explícito. */
public record AsignacionRol(
        UUID id,
        TenantId tenantId,
        UsuarioId userId,
        RolId roleId,
        AmbitoOrganizacional scope,
        Instant validFrom,
        Instant validUntil,
        EstadoAsignacionRol status,
        String createdBy,
        Instant createdAt) {

    public static Result<AsignacionRol, ErrorDetail> create(
            UUID id,
            UUID tenantId,
            UUID userId,
            UUID roleId,
            TipoAmbito scopeType,
            UUID companyId,
            UUID establishmentId,
            UUID warehouseId,
            UUID terminalId,
            Instant validFrom,
            Instant validUntil,
            String createdBy,
            Instant createdAt) {
        if (id == null) return invalid("id", "La identidad de asignación es obligatoria.");
        if (tenantId == null) return invalid("tenantId", "El tenant es obligatorio.");
        if (userId == null) return invalid("userId", "El usuario es obligatorio.");
        if (roleId == null) return invalid("roleId", "El rol es obligatorio.");
        if (scopeType == null) return invalid("scopeType", "El tipo de ámbito es obligatorio.");
        if (!validScope(scopeType, companyId, establishmentId, warehouseId, terminalId)) {
            return invalid("scope", "Los identificadores no corresponden al tipo de ámbito indicado.");
        }
        if (validFrom == null) return invalid("validFrom", "El inicio de vigencia es obligatorio.");
        if (validUntil != null && validUntil.isBefore(validFrom)) {
            return invalid("validUntil", "El fin de vigencia no puede ser anterior al inicio.");
        }
        if (createdAt == null) return invalid("createdAt", "El instante de registro es obligatorio.");
        var normalizedCreatedBy = createdBy == null ? null : createdBy.trim();
        if (normalizedCreatedBy != null && normalizedCreatedBy.length() > 100) {
            return invalid("createdBy", "El actor no debe exceder 100 caracteres.");
        }
        return Result.success(new AsignacionRol(
                id,
                new TenantId(tenantId),
                new UsuarioId(userId),
                new RolId(roleId),
                new AmbitoOrganizacional(scopeType, companyId, establishmentId, warehouseId, terminalId),
                validFrom,
                validUntil,
                EstadoAsignacionRol.ACTIVO,
                normalizedCreatedBy,
                createdAt));
    }

    private static boolean validScope(
            TipoAmbito type, UUID companyId, UUID establishmentId, UUID warehouseId, UUID terminalId) {
        return switch (type) {
            case GLOBAL -> companyId == null && establishmentId == null && warehouseId == null && terminalId == null;
            case EMPRESA -> companyId != null && establishmentId == null && warehouseId == null && terminalId == null;
            case ESTABLECIMIENTO -> companyId != null && establishmentId != null
                    && warehouseId == null && terminalId == null;
            case ALMACEN -> companyId != null && establishmentId != null
                    && warehouseId != null && terminalId == null;
            case TERMINAL -> companyId != null && establishmentId != null
                    && warehouseId == null && terminalId != null;
        };
    }

    private static Result<AsignacionRol, ErrorDetail> invalid(String field, String message) {
        return Result.failure(new ErrorDetail("SEC_ASIGNACION_INVALIDA", message, Map.of("field", field)));
    }
}
