package com.softprimesolutions.security.application.mapper;

import com.softprimesolutions.security.application.dto.result.AsignacionRolResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;

public final class IamApplicationMapper {

    private IamApplicationMapper() {
    }

    public static UsuarioResult toResult(Identidad identidad, Usuario user) {
        return new UsuarioResult(
                user.id().value(),
                user.tenantId().value(),
                identidad.documentType(),
                identidad.documentNumber(),
                identidad.firstNames(),
                identidad.lastNames(),
                identidad.username(),
                identidad.email(),
                user.displayName(),
                identidad.phone(),
                user.credentialChangeRequired(),
                user.mfaRequired(),
                user.status().name(),
                user.createdAt(),
                user.updatedAt());
    }

    public static RolResult toResult(Rol role) {
        return new RolResult(
                role.id().value(),
                role.tenantId().value(),
                role.code(),
                role.name(),
                role.description(),
                role.roleType().name(),
                role.systemRole(),
                role.permissionCodes(),
                role.status().name(),
                role.createdAt(),
                role.updatedAt());
    }

    public static AsignacionRolResult toResult(AsignacionRol assignment) {
        return new AsignacionRolResult(
                assignment.id(),
                assignment.tenantId().value(),
                assignment.userId().value(),
                assignment.roleId().value(),
                assignment.scope().type().name(),
                assignment.scope().companyId(),
                assignment.scope().establishmentId(),
                assignment.scope().warehouseId(),
                assignment.scope().terminalId(),
                assignment.validFrom(),
                assignment.validUntil(),
                assignment.status().name(),
                assignment.createdBy(),
                assignment.createdAt());
    }
}
