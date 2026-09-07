package com.softprimesolutions.security.infrastructure.persistence.read.mapper;

import com.softprimesolutions.security.application.dto.result.PermisoResult;
import com.softprimesolutions.security.application.dto.result.RolResult;
import com.softprimesolutions.security.application.dto.result.UsuarioResult;
import com.softprimesolutions.security.infrastructure.persistence.read.projection.PermisoProjection;
import com.softprimesolutions.security.infrastructure.persistence.read.projection.RolProjection;
import com.softprimesolutions.security.infrastructure.persistence.read.projection.UsuarioProjection;

public final class IamReadMapper {

    private IamReadMapper() {
    }

    public static UsuarioResult toResult(UsuarioProjection value) {
        return new UsuarioResult(
                value.id(), value.tenantId(), value.documentType(), value.documentNumber(),
                value.firstNames(), value.lastNames(), value.username(), value.email(),
                value.displayName(), value.phone(), value.credentialChangeRequired(), value.mfaRequired(),
                value.status(), value.createdAt(), value.updatedAt());
    }

    public static RolResult toResult(RolProjection value) {
        return new RolResult(
                value.id(), value.tenantId(), value.code(), value.name(), value.description(),
                value.roleType(), value.systemRole(), value.permissionCodes(), value.status(),
                value.createdAt(), value.updatedAt());
    }

    public static PermisoResult toResult(PermisoProjection value) {
        return new PermisoResult(
                value.moduleCode(), value.moduleName(), value.code(), value.resource(), value.action(),
                value.name(), value.description(), value.critical(), value.status());
    }
}
