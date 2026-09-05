package com.softprimesolutions.security.infrastructure.persistence.write.mapper;

import com.softprimesolutions.security.domain.model.EstadoRol;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.domain.valueobject.RolId;
import com.softprimesolutions.security.domain.valueobject.TenantId;
import com.softprimesolutions.security.infrastructure.persistence.write.entity.AsignacionRolJpaEntity;
import com.softprimesolutions.security.infrastructure.persistence.write.entity.RolJpaEntity;
import com.softprimesolutions.security.infrastructure.persistence.write.entity.UsuarioJpaEntity;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import java.util.Set;
import java.util.UUID;

public final class IamWriteMapper {

    private IamWriteMapper() {
    }

    public static UsuarioJpaEntity toEntity(Usuario user, Long tenantId) {
        return new UsuarioJpaEntity(
                user.id().value(), tenantId, user.documentType(), user.documentNumber(),
                user.firstNames(), user.lastNames(), user.username(), user.email(), user.phone(),
                user.displayName(), user.credentialChangeRequired(), user.mfaRequired(),
                user.status().name(), user.createdAt(), user.updatedAt());
    }

    public static RolJpaEntity toEntity(Rol role, Long tenantId) {
        return new RolJpaEntity(
                role.id().value(), tenantId, role.code(), role.name(), role.description(),
                role.roleType().name(), role.systemRole(), role.status().name(),
                role.createdAt(), role.updatedAt());
    }

    public static Rol toDomain(RolJpaEntity entity, UUID tenantUuid, Set<String> permissionCodes) {
        return Rol.restore(
                new RolId(entity.getUuidPublico()), new TenantId(tenantUuid),
                entity.getCodigo(), entity.getNombre(), entity.getDescripcion(),
                com.softprimesolutions.security.domain.model.TipoRol.valueOf(entity.getTipoRol()),
                entity.isEsSistema(), permissionCodes, EstadoRol.valueOf(entity.getEstado()),
                entity.getCreatedAt(), entity.getUpdatedAt());
    }

    public static AsignacionRolJpaEntity toEntity(
            AsignacionRol assignment,
            Long tenantId,
            Long usuarioId,
            Long rolId,
            Long empresaId,
            Long establecimientoId,
            Long almacenId,
            Long terminalId) {
        return new AsignacionRolJpaEntity(
                assignment.id(), tenantId, usuarioId, rolId, assignment.scope().type().name(),
                empresaId, establecimientoId, almacenId, terminalId,
                assignment.validFrom(), assignment.validUntil(), assignment.status().name(),
                assignment.createdBy(), assignment.createdAt());
    }
}
