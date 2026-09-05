package com.softprimesolutions.security.api.mapper;

import com.softprimesolutions.security.api.dto.request.AsignarRolUsuarioRequest;
import com.softprimesolutions.security.api.dto.request.CrearRolRequest;
import com.softprimesolutions.security.api.dto.request.CrearUsuarioRequest;
import com.softprimesolutions.security.api.dto.request.ReemplazarPermisosRolRequest;
import com.softprimesolutions.security.api.dto.response.AsignacionRolResponse;
import com.softprimesolutions.security.api.dto.response.PaginaResponse;
import com.softprimesolutions.security.api.dto.response.PermisoResponse;
import com.softprimesolutions.security.api.dto.response.RolResponse;
import com.softprimesolutions.security.api.dto.response.UsuarioResponse;
import com.softprimesolutions.security.application.dto.command.AsignarRolUsuarioCommand;
import com.softprimesolutions.security.application.dto.command.CrearRolCommand;
import com.softprimesolutions.security.application.dto.command.CrearUsuarioCommand;
import com.softprimesolutions.security.application.dto.command.ReemplazarPermisosRolCommand;
import com.softprimesolutions.security.application.dto.result.AsignacionRolResult;
import com.softprimesolutions.security.application.dto.result.PaginaResult;
import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import java.util.Set;
import java.util.UUID;

public final class IamApiMapper {

    private IamApiMapper() {
    }

    public static CrearUsuarioCommand toCommand(CrearUsuarioRequest request) {
        return new CrearUsuarioCommand(
                request.tenantId(), request.identityProvider(), request.identitySubject(),
                request.identityIssuer(), request.emailClaim(), request.documentType(), request.documentNumber(),
                request.firstNames(), request.lastNames(), request.username(), request.email(), request.phone(),
                request.displayName(), Boolean.TRUE.equals(request.credentialChangeRequired()),
                Boolean.TRUE.equals(request.mfaRequired()));
    }

    public static CrearRolCommand toCommand(CrearRolRequest request) {
        return new CrearRolCommand(
                request.tenantId(), request.code(), request.name(), request.description(),
                request.roleType(), Boolean.TRUE.equals(request.systemRole()));
    }

    public static ReemplazarPermisosRolCommand toCommand(
            UUID roleId, ReemplazarPermisosRolRequest request, String grantedBy) {
        return new ReemplazarPermisosRolCommand(roleId, Set.copyOf(request.permissionCodes()), grantedBy);
    }

    public static AsignarRolUsuarioCommand toCommand(
            UUID userId, AsignarRolUsuarioRequest request, String createdBy) {
        return new AsignarRolUsuarioCommand(
                request.tenantId(), userId, request.roleId(), request.scopeType(),
                request.companyId(), request.establishmentId(), request.warehouseId(), request.terminalId(),
                request.validFrom(), request.validUntil(), createdBy);
    }

    public static UsuarioResponse toResponse(UsuarioResult result) {
        return new UsuarioResponse(
                result.id(), result.tenantId(), result.identityProvider(), result.identityIssuer(),
                result.identitySubject(), result.emailClaim(), result.documentType(), result.documentNumber(),
                result.firstNames(), result.lastNames(), result.username(), result.email(), result.displayName(),
                result.phone(), result.credentialChangeRequired(), result.mfaRequired(), result.status(),
                result.createdAt(), result.updatedAt());
    }

    public static RolResponse toResponse(RolResult result) {
        return new RolResponse(
                result.id(), result.tenantId(), result.code(), result.name(), result.description(),
                result.roleType(), result.systemRole(), result.permissionCodes(), result.status(),
                result.createdAt(), result.updatedAt());
    }

    public static PermisoResponse toResponse(PermisoResult result) {
        return new PermisoResponse(
                result.moduleCode(), result.moduleName(), result.code(), result.resource(), result.action(),
                result.name(), result.description(), result.critical(), result.status());
    }

    public static AsignacionRolResponse toResponse(AsignacionRolResult result) {
        return new AsignacionRolResponse(
                result.id(), result.tenantId(), result.userId(), result.roleId(), result.scopeType(),
                result.companyId(), result.establishmentId(), result.warehouseId(), result.terminalId(),
                result.validFrom(), result.validUntil(), result.status(), result.createdBy(), result.createdAt());
    }

    public static PaginaResponse<UsuarioResponse> toUsuarioPage(PaginaResult<UsuarioResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(IamApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }

    public static PaginaResponse<RolResponse> toRolPage(PaginaResult<RolResult> result) {
        return new PaginaResponse<>(
                result.items().stream().map(IamApiMapper::toResponse).toList(),
                result.page(), result.size(), result.totalElements());
    }
}
