package com.softprimesolutions.security.infrastructure.persistence.write.adapter;

import com.softprimesolutions.security.application.port.out.IamWritePort;
import com.softprimesolutions.security.domain.model.AsignacionRol;
import com.softprimesolutions.security.domain.model.Identidad;
import com.softprimesolutions.security.domain.model.Rol;
import com.softprimesolutions.security.domain.model.Usuario;
import com.softprimesolutions.security.infrastructure.persistence.write.mapper.IamWriteMapper;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.AsignacionRolJpaRepository;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.IdentidadJpaRepository;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.MembershipJpaRepository;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.PermisoJpaRepository;
import com.softprimesolutions.security.infrastructure.persistence.write.repository.RolJpaRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class IamJpaWriteAdapter implements IamWritePort {

    private final IdentidadJpaRepository identidadRepository;
    private final MembershipJpaRepository membershipRepository;
    private final RolJpaRepository roleRepository;
    private final PermisoJpaRepository permissionRepository;
    private final AsignacionRolJpaRepository assignmentRepository;
    private final JdbcClient jdbcClient;

    public IamJpaWriteAdapter(
            IdentidadJpaRepository identidadRepository,
            MembershipJpaRepository membershipRepository,
            RolJpaRepository roleRepository,
            PermisoJpaRepository permissionRepository,
            AsignacionRolJpaRepository assignmentRepository,
            JdbcClient jdbcClient) {
        this.identidadRepository = identidadRepository;
        this.membershipRepository = membershipRepository;
        this.roleRepository = roleRepository;
        this.permissionRepository = permissionRepository;
        this.assignmentRepository = assignmentRepository;
        this.jdbcClient = jdbcClient;
    }

    @Override
    @Transactional
    public SaveUsuarioOutcome save(Identidad identidad, Usuario user) {
        var tenantId = findTenantId(user.tenantId().value());
        if (tenantId.isEmpty()) return SaveUsuarioOutcome.TENANT_NOT_FOUND;
        if (identidadRepository.existsByEmail(identidad.email())) return SaveUsuarioOutcome.DUPLICATE_EMAIL;
        if (identidad.username() != null && identidadRepository.existsByUsername(identidad.username())) {
            return SaveUsuarioOutcome.DUPLICATE_USERNAME;
        }
        if (identidad.documentNumber() != null && identidadRepository.existsByTipoDocumentoAndNumeroDocumento(
                identidad.documentType(), identidad.documentNumber())) {
            return SaveUsuarioOutcome.DUPLICATE_DOCUMENT;
        }
        try {
            var identidadEntity = identidadRepository.saveAndFlush(IamWriteMapper.toEntity(identidad));
            membershipRepository.saveAndFlush(
                    IamWriteMapper.toEntity(user, tenantId.get(), identidadEntity.getId()));
            return SaveUsuarioOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveUsuarioOutcome.DUPLICATE_CONSTRAINT;
        }
    }

    @Override
    @Transactional
    public SaveRolOutcome save(Rol role) {
        var tenantId = findTenantId(role.tenantId().value());
        if (tenantId.isEmpty()) return SaveRolOutcome.TENANT_NOT_FOUND;
        var existing = roleRepository.findByUuidPublico(role.id().value());
        if (existing.isEmpty() && roleRepository.existsByTenantIdAndCodigo(tenantId.get(), role.code())) {
            return SaveRolOutcome.DUPLICATE_CODE;
        }
        try {
            if (existing.isPresent()) {
                jdbcClient.sql("""
                                UPDATE sch_seguridad.rol
                                   SET nombre = :name, descripcion = :description, tipo_rol = :roleType,
                                       es_sistema = :systemRole, estado = :status, updated_at = :updatedAt
                                 WHERE uuid_publico = :roleId
                                """)
                        .param("name", role.name())
                        .param("description", role.description())
                        .param("roleType", role.roleType().name())
                        .param("systemRole", role.systemRole())
                        .param("status", role.status().name())
                        .param("updatedAt", toOffsetDateTime(role.updatedAt()))
                        .param("roleId", role.id().value())
                        .update();
                return SaveRolOutcome.UPDATED;
            }
            roleRepository.saveAndFlush(IamWriteMapper.toEntity(role, tenantId.get()));
            return SaveRolOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveRolOutcome.DUPLICATE_CODE;
        }
    }

    @Override
    @Transactional
    public SaveRolOutcome replacePermissions(Rol role, String grantedBy, Instant grantedAt) {
        var entity = roleRepository.findByUuidPublico(role.id().value());
        if (entity.isEmpty()) return SaveRolOutcome.DUPLICATE_CODE;

        jdbcClient.sql("DELETE FROM sch_seguridad.rol_permiso WHERE rol_id = :roleId")
                .param("roleId", entity.get().getId())
                .update();
        for (var permissionCode : role.permissionCodes()) {
            jdbcClient.sql("""
                            INSERT INTO sch_seguridad.rol_permiso
                                (tenant_id, rol_id, permiso_id, estado, granted_at, granted_by)
                            SELECT :tenantId, :roleId, p.id, 'ACTIVO', :grantedAt, :grantedBy
                              FROM sch_seguridad.permiso p
                             WHERE p.codigo = :permissionCode
                            """)
                    .param("tenantId", entity.get().getTenantId())
                    .param("roleId", entity.get().getId())
                    .param("grantedAt", toOffsetDateTime(grantedAt))
                    .param("grantedBy", grantedBy)
                    .param("permissionCode", permissionCode)
                    .update();
        }
        jdbcClient.sql("UPDATE sch_seguridad.rol SET updated_at = :updatedAt WHERE id = :roleId")
                .param("updatedAt", toOffsetDateTime(grantedAt))
                .param("roleId", entity.get().getId())
                .update();
        return SaveRolOutcome.UPDATED;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Rol> findRole(UUID roleId) {
        return roleRepository.findByUuidPublico(roleId).flatMap(entity ->
                findTenantUuid(entity.getTenantId()).map(tenantUuid ->
                        IamWriteMapper.toDomain(entity, tenantUuid, findPermissionCodes(entity.getId()))));
    }

    @Override
    @Transactional(readOnly = true)
    public boolean allPermissionsExist(Set<String> permissionCodes) {
        return permissionCodes.isEmpty()
                || permissionRepository.countByCodigoIn(permissionCodes) == permissionCodes.size();
    }

    @Override
    public boolean tenantExists(UUID tenantId) {
        return findTenantId(tenantId).isPresent();
    }

    @Override
    public boolean userBelongsToTenant(UUID userId, UUID tenantId) {
        return membershipRepository.findByUuidPublico(userId)
                .map(membership -> findTenantId(tenantId).filter(membership.getTenantId()::equals).isPresent())
                .orElse(false);
    }

    @Override
    public boolean roleBelongsToTenant(UUID roleId, UUID tenantId) {
        return roleRepository.findByUuidPublico(roleId)
                .map(role -> findTenantId(tenantId).filter(role.getTenantId()::equals).isPresent())
                .orElse(false);
    }

    @Override
    public boolean scopeExists(
            UUID tenantId,
            com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional scope) {
        return resolveScope(tenantId, scope).isPresent();
    }

    @Override
    @Transactional
    public SaveAssignmentOutcome save(AsignacionRol assignment) {
        var tenantId = findTenantId(assignment.tenantId().value());
        var user = membershipRepository.findByUuidPublico(assignment.userId().value());
        var role = roleRepository.findByUuidPublico(assignment.roleId().value());
        var scope = resolveScope(assignment.tenantId().value(), assignment.scope());
        if (tenantId.isEmpty() || user.isEmpty() || role.isEmpty() || scope.isEmpty()) {
            return SaveAssignmentOutcome.DUPLICATE;
        }
        var resolvedScope = scope.get();
        var duplicateCount = jdbcClient.sql("""
                        SELECT COUNT(*)
                          FROM sch_seguridad.usuario_rol_ambito
                         WHERE tenant_id = :tenantId
                           AND membership_id = :userId
                           AND rol_id = :roleId
                           AND tipo_ambito = :scopeType
                           AND COALESCE(empresa_id, 0) = :companyId
                           AND COALESCE(establecimiento_id, 0) = :establishmentId
                           AND COALESCE(almacen_id, 0) = :warehouseId
                           AND COALESCE(terminal_id, 0) = :terminalId
                           AND estado = 'ACTIVO'
                        """)
                .param("tenantId", tenantId.get())
                .param("userId", user.get().getId())
                .param("roleId", role.get().getId())
                .param("scopeType", assignment.scope().type().name())
                .param("companyId", valueOrZero(resolvedScope.companyId()))
                .param("establishmentId", valueOrZero(resolvedScope.establishmentId()))
                .param("warehouseId", valueOrZero(resolvedScope.warehouseId()))
                .param("terminalId", valueOrZero(resolvedScope.terminalId()))
                .query(Long.class)
                .single();
        if (duplicateCount > 0) {
            return SaveAssignmentOutcome.DUPLICATE;
        }
        try {
            assignmentRepository.saveAndFlush(IamWriteMapper.toEntity(
                    assignment, tenantId.get(), user.get().getId(), role.get().getId(),
                    resolvedScope.companyId(), resolvedScope.establishmentId(),
                    resolvedScope.warehouseId(), resolvedScope.terminalId()));
            return SaveAssignmentOutcome.CREATED;
        } catch (DataIntegrityViolationException exception) {
            return SaveAssignmentOutcome.DUPLICATE;
        }
    }

    private Optional<Long> findTenantId(UUID tenantUuid) {
        if (tenantUuid == null) return Optional.empty();
        return jdbcClient.sql("SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantUuid")
                .param("tenantUuid", tenantUuid)
                .query(Long.class)
                .optional();
    }

    private Optional<UUID> findTenantUuid(Long tenantId) {
        return jdbcClient.sql("SELECT uuid_publico FROM sch_farmacia.tenant WHERE id = :tenantId")
                .param("tenantId", tenantId)
                .query(UUID.class)
                .optional();
    }

    private Set<String> findPermissionCodes(Long roleId) {
        return jdbcClient.sql("""
                        SELECT p.codigo
                          FROM sch_seguridad.rol_permiso rp
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                         WHERE rp.rol_id = :roleId AND rp.estado = 'ACTIVO'
                         ORDER BY p.codigo
                        """)
                .param("roleId", roleId)
                .query(String.class)
                .list()
                .stream()
                .collect(java.util.stream.Collectors.toCollection(LinkedHashSet::new));
    }

    private Optional<ResolvedScope> resolveScope(
            UUID tenantUuid,
            com.softprimesolutions.security.domain.valueobject.AmbitoOrganizacional scope) {
        if (tenantUuid == null || scope == null || scope.type() == null) return Optional.empty();
        return switch (scope.type()) {
            case GLOBAL -> findTenantId(tenantUuid).map(ignored -> new ResolvedScope(null, null, null, null));
            case EMPRESA -> jdbcClient.sql("""
                            SELECT e.id
                              FROM sch_farmacia.empresa_operadora e
                              JOIN sch_farmacia.tenant t ON t.id = e.tenant_id
                             WHERE t.uuid_publico = :tenantUuid AND e.uuid_publico = :companyUuid
                            """)
                    .param("tenantUuid", tenantUuid)
                    .param("companyUuid", scope.companyId())
                    .query(Long.class)
                    .optional()
                    .map(companyId -> new ResolvedScope(companyId, null, null, null));
            case ESTABLECIMIENTO -> jdbcClient.sql("""
                            SELECT e.id AS company_id, s.id AS establishment_id
                              FROM sch_farmacia.establecimiento_farmaceutico s
                              JOIN sch_farmacia.empresa_operadora e ON e.id = s.empresa_id AND e.tenant_id = s.tenant_id
                              JOIN sch_farmacia.tenant t ON t.id = s.tenant_id
                             WHERE t.uuid_publico = :tenantUuid
                               AND e.uuid_publico = :companyUuid
                               AND s.uuid_publico = :establishmentUuid
                            """)
                    .param("tenantUuid", tenantUuid)
                    .param("companyUuid", scope.companyId())
                    .param("establishmentUuid", scope.establishmentId())
                    .query((rs, rowNumber) -> new ResolvedScope(
                            rs.getLong("company_id"), rs.getLong("establishment_id"), null, null))
                    .optional();
            case ALMACEN -> jdbcClient.sql("""
                            SELECT e.id AS company_id, s.id AS establishment_id, a.id AS warehouse_id
                              FROM sch_farmacia.almacen a
                              JOIN sch_farmacia.establecimiento_farmaceutico s
                                ON s.id = a.establecimiento_id AND s.empresa_id = a.empresa_id AND s.tenant_id = a.tenant_id
                              JOIN sch_farmacia.empresa_operadora e ON e.id = a.empresa_id AND e.tenant_id = a.tenant_id
                              JOIN sch_farmacia.tenant t ON t.id = a.tenant_id
                             WHERE t.uuid_publico = :tenantUuid
                               AND e.uuid_publico = :companyUuid
                               AND s.uuid_publico = :establishmentUuid
                               AND a.uuid_publico = :warehouseUuid
                            """)
                    .param("tenantUuid", tenantUuid)
                    .param("companyUuid", scope.companyId())
                    .param("establishmentUuid", scope.establishmentId())
                    .param("warehouseUuid", scope.warehouseId())
                    .query((rs, rowNumber) -> new ResolvedScope(
                            rs.getLong("company_id"), rs.getLong("establishment_id"),
                            rs.getLong("warehouse_id"), null))
                    .optional();
            case TERMINAL -> jdbcClient.sql("""
                            SELECT e.id AS company_id, s.id AS establishment_id, p.id AS terminal_id
                              FROM sch_farmacia.terminal_pos p
                              JOIN sch_farmacia.establecimiento_farmaceutico s
                                ON s.id = p.establecimiento_id AND s.empresa_id = p.empresa_id AND s.tenant_id = p.tenant_id
                              JOIN sch_farmacia.empresa_operadora e ON e.id = p.empresa_id AND e.tenant_id = p.tenant_id
                              JOIN sch_farmacia.tenant t ON t.id = p.tenant_id
                             WHERE t.uuid_publico = :tenantUuid
                               AND e.uuid_publico = :companyUuid
                               AND s.uuid_publico = :establishmentUuid
                               AND p.uuid_publico = :terminalUuid
                            """)
                    .param("tenantUuid", tenantUuid)
                    .param("companyUuid", scope.companyId())
                    .param("establishmentUuid", scope.establishmentId())
                    .param("terminalUuid", scope.terminalId())
                    .query((rs, rowNumber) -> new ResolvedScope(
                            rs.getLong("company_id"), rs.getLong("establishment_id"),
                            null, rs.getLong("terminal_id")))
                    .optional();
        };
    }

    private static long valueOrZero(Long value) {
        return value == null ? 0L : value;
    }

    private static OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private record ResolvedScope(Long companyId, Long establishmentId, Long warehouseId, Long terminalId) {
    }
}
