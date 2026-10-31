package com.softprimesolutions.security.infrastructure.persistence.read.adapter;

import com.softprimesolutions.security.application.port.out.SecurityControlPort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SecurityControlJdbcAdapter implements SecurityControlPort {

    private final JdbcClient jdbc;

    public SecurityControlJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional
    public boolean updateUserStatus(UUID tenantId, UUID userId, String status, Instant changedAt) {
        return jdbc.sql("""
                        UPDATE sch_seguridad.membership m
                           SET estado = :status, updated_at = :changedAt
                          FROM sch_admin.tenant t
                         WHERE t.id = m.tenant_id
                           AND t.uuid_publico = :tenantId
                           AND m.uuid_publico = :userId
                        """)
                .param("status", status).param("changedAt", toOffsetDateTime(changedAt))
                .param("tenantId", tenantId).param("userId", userId).update() == 1;
    }

    @Override
    @Transactional
    public boolean updateRoleStatus(UUID tenantId, UUID roleId, String status, Instant changedAt) {
        return jdbc.sql("""
                        UPDATE sch_seguridad.rol r
                           SET estado = :status, updated_at = :changedAt
                          FROM sch_admin.tenant t
                         WHERE t.id = r.tenant_id
                           AND t.uuid_publico = :tenantId
                           AND r.uuid_publico = :roleId
                        """)
                .param("status", status).param("changedAt", toOffsetDateTime(changedAt))
                .param("tenantId", tenantId).param("roleId", roleId).update() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExternalIdentityView> findExternalIdentities(UUID tenantId, UUID userId) {
        return jdbc.sql("""
                        SELECT i.provider, i.subject, i.issuer, CAST(i.email_claim AS VARCHAR) AS email_claim,
                               i.ultimo_login_at, i.created_at
                          FROM sch_seguridad.identidad_externa i
                          JOIN sch_seguridad.membership m ON m.identidad_id = i.identidad_id
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                      ORDER BY i.created_at, i.id
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .query((rs, row) -> new ExternalIdentityView(
                        rs.getString("provider"), rs.getString("subject"), rs.getString("issuer"),
                        rs.getString("email_claim"), instant(rs, "ultimo_login_at"), instant(rs, "created_at")))
                .list();
    }

    @Override
    @Transactional
    public boolean createExternalIdentity(
            UUID tenantId, UUID userId, ExternalIdentityData data, Instant createdAt) {
        try {
            var query = jdbc.sql("""
                            INSERT INTO sch_seguridad.identidad_externa
                                (identidad_id, provider, subject, issuer, email_claim, created_at)
                            SELECT m.identidad_id, :provider, :subject, :issuer, :emailClaim, :createdAt
                              FROM sch_seguridad.membership m
                              JOIN sch_admin.tenant t ON t.id = m.tenant_id
                             WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                            """)
                    .param("provider", data.provider()).param("subject", data.subject())
                    .param("createdAt", toOffsetDateTime(createdAt)).param("tenantId", tenantId).param("userId", userId);
            query = data.issuer() == null
                    ? query.param("issuer", null, Types.VARCHAR) : query.param("issuer", data.issuer());
            query = data.emailClaim() == null
                    ? query.param("emailClaim", null, Types.VARCHAR) : query.param("emailClaim", data.emailClaim());
            return query.update() == 1;
        } catch (DuplicateKeyException duplicate) {
            return false;
        }
    }

    @Override
    @Transactional
    public boolean deleteExternalIdentity(UUID tenantId, UUID userId, String provider, String subject) {
        return jdbc.sql("""
                        DELETE FROM sch_seguridad.identidad_externa i
                         WHERE i.provider = :provider AND i.subject = :subject
                           AND EXISTS (
                               SELECT 1 FROM sch_seguridad.membership m
                               JOIN sch_admin.tenant t ON t.id = m.tenant_id
                                WHERE m.identidad_id = i.identidad_id AND m.uuid_publico = :userId
                                  AND t.uuid_publico = :tenantId
                           )
                           AND EXISTS (
                               SELECT 1 FROM sch_seguridad.identidad_externa other
                                WHERE other.identidad_id = i.identidad_id AND other.id <> i.id
                           )
                        """)
                .param("provider", provider).param("subject", subject)
                .param("tenantId", tenantId).param("userId", userId).update() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<AssignmentView> findAssignments(UUID tenantId, UUID userId) {
        return jdbc.sql("""
                        SELECT a.uuid_publico, r.uuid_publico AS rol_uuid, r.codigo AS rol_codigo,
                               r.nombre AS rol_nombre, a.tipo_ambito,
                               e.uuid_publico AS empresa_uuid, s.uuid_publico AS establecimiento_uuid,
                               w.uuid_publico AS almacen_uuid, p.uuid_publico AS terminal_uuid,
                               a.vigente_desde, a.vigente_hasta, a.estado, a.created_by, a.created_at
                          FROM sch_seguridad.usuario_rol_ambito a
                          JOIN sch_seguridad.membership m ON m.id = a.membership_id AND m.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
                          JOIN sch_admin.tenant t ON t.id = a.tenant_id
                     LEFT JOIN sch_organizacion.empresa_operadora e ON e.id = a.empresa_id
                     LEFT JOIN sch_organizacion.establecimiento_farmaceutico s ON s.id = a.establecimiento_id
                     LEFT JOIN sch_organizacion.almacen w ON w.id = a.almacen_id
                     LEFT JOIN sch_organizacion.terminal_pos p ON p.id = a.terminal_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                      ORDER BY a.created_at DESC, a.id DESC
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .query((rs, row) -> assignment(rs)).list();
    }

    @Override
    @Transactional
    public boolean revokeAssignment(UUID tenantId, UUID userId, UUID assignmentId) {
        return jdbc.sql("""
                        UPDATE sch_seguridad.usuario_rol_ambito a
                           SET estado = 'REVOCADO'
                         WHERE a.uuid_publico = :assignmentId AND a.estado = 'ACTIVO'
                           AND EXISTS (
                               SELECT 1
                                 FROM sch_seguridad.membership m
                                 JOIN sch_admin.tenant t ON t.id = m.tenant_id
                                WHERE m.id = a.membership_id AND m.tenant_id = a.tenant_id
                                  AND t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           )
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .param("assignmentId", assignmentId).update() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findEffectivePermissions(UUID tenantId, UUID userId, Instant at) {
        return new LinkedHashSet<>(jdbc.sql("""
                        SELECT DISTINCT p.codigo
                          FROM sch_seguridad.usuario_rol_ambito a
                          JOIN sch_seguridad.membership m ON m.id = a.membership_id AND m.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol_permiso rp ON rp.rol_id = r.id AND rp.tenant_id = r.tenant_id
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                          JOIN sch_admin.tenant t ON t.id = a.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           AND m.estado = 'ACTIVO' AND r.estado = 'ACTIVO'
                           AND a.estado = 'ACTIVO' AND rp.estado = 'ACTIVO' AND p.estado = 'ACTIVO'
                           AND a.vigente_desde <= :at
                           AND (a.vigente_hasta IS NULL OR a.vigente_hasta >= :at)
                      ORDER BY p.codigo
                        """)
                .param("tenantId", tenantId).param("userId", userId).param("at", toOffsetDateTime(at))
                .query(String.class).list());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ModuleView> findModules() {
        return jdbc.sql("""
                        SELECT codigo, nombre, descripcion, orden, es_activo
                          FROM sch_seguridad.modulo_sistema
                      ORDER BY orden, codigo
                        """)
                .query((rs, row) -> new ModuleView(rs.getString("codigo"), rs.getString("nombre"),
                        rs.getString("descripcion"), rs.getInt("orden"), rs.getBoolean("es_activo")))
                .list();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SessionView> findSessions(UUID tenantId, UUID userId) {
        var sql = """
                SELECT s.uuid_sesion, m.uuid_publico AS usuario_uuid, s.provider, s.auth_method,
                       s.canal, CAST(s.ip_origen AS VARCHAR) AS ip_origen, s.user_agent,
                       s.dispositivo_ref, s.login_at, s.ultimo_uso_at, s.expira_at,
                       s.logout_at, s.revocado_at, s.motivo_revocacion, s.estado
                  FROM sch_seguridad.sesion_usuario s
                  JOIN sch_seguridad.membership m ON m.id = s.membership_id AND m.tenant_id = s.tenant_id
                  JOIN sch_admin.tenant t ON t.id = s.tenant_id
                 WHERE t.uuid_publico = :tenantId
                """ + (userId == null ? "" : " AND m.uuid_publico = :userId")
                + " ORDER BY s.login_at DESC, s.id DESC";
        var query = jdbc.sql(sql).param("tenantId", tenantId);
        if (userId != null) query = query.param("userId", userId);
        return query.query((rs, row) -> session(rs)).list();
    }

    @Override
    @Transactional
    public boolean revokeSession(UUID tenantId, UUID sessionId, String reason, Instant revokedAt) {
        var query = jdbc.sql("""
                        UPDATE sch_seguridad.sesion_usuario s
                           SET estado = 'REVOCADA', revocado_at = :revokedAt, motivo_revocacion = :reason
                          FROM sch_admin.tenant t
                         WHERE t.id = s.tenant_id AND t.uuid_publico = :tenantId
                           AND s.uuid_sesion = :sessionId AND s.estado = 'ACTIVA'
                        """)
                .param("revokedAt", toOffsetDateTime(revokedAt)).param("tenantId", tenantId).param("sessionId", sessionId);
        query = reason == null ? query.param("reason", null, Types.VARCHAR) : query.param("reason", reason);
        return query.update() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public List<DeviceView> findDevices(UUID tenantId) {
        return jdbc.sql("""
                        SELECT d.uuid_publico, e.uuid_publico AS empresa_uuid,
                               s.uuid_publico AS establecimiento_uuid, p.uuid_publico AS terminal_uuid,
                               d.device_fingerprint_hash, d.certificado_thumbprint, d.version_agente,
                               d.estado, d.registrado_at, d.ultimo_contacto_at
                          FROM sch_seguridad.dispositivo_tienda d
                          JOIN sch_admin.tenant t ON t.id = d.tenant_id
                          JOIN sch_organizacion.empresa_operadora e ON e.id = d.empresa_id
                          JOIN sch_organizacion.establecimiento_farmaceutico s ON s.id = d.establecimiento_id
                     LEFT JOIN sch_organizacion.terminal_pos p ON p.id = d.terminal_id
                         WHERE t.uuid_publico = :tenantId
                      ORDER BY d.registrado_at DESC, d.id DESC
                        """)
                .param("tenantId", tenantId).query((rs, row) -> device(rs)).list();
    }

    @Override
    @Transactional
    public boolean createDevice(RegisterDeviceData data, UUID deviceId, Instant registeredAt) {
        var terminalJoin = data.terminalId() == null
                ? "" : " JOIN sch_organizacion.terminal_pos p ON p.tenant_id = t.id AND p.empresa_id = e.id"
                        + " AND p.establecimiento_id = s.id AND p.uuid_publico = :terminalId";
        var terminalValue = data.terminalId() == null ? "NULL" : "p.id";
        var query = jdbc.sql("""
                        INSERT INTO sch_seguridad.dispositivo_tienda
                            (uuid_publico, tenant_id, empresa_id, establecimiento_id, terminal_id,
                             device_fingerprint_hash, certificado_thumbprint, version_agente, estado, registrado_at)
                        SELECT :deviceId, t.id, e.id, s.id, %s,
                               :fingerprint, :thumbprint, :agentVersion, 'PENDIENTE', :registeredAt
                          FROM sch_admin.tenant t
                          JOIN sch_organizacion.empresa_operadora e
                            ON e.tenant_id = t.id AND e.uuid_publico = :companyId
                          JOIN sch_organizacion.establecimiento_farmaceutico s
                            ON s.tenant_id = t.id AND s.empresa_id = e.id AND s.uuid_publico = :establishmentId
                          %s
                         WHERE t.uuid_publico = :tenantId
                        """.formatted(terminalValue, terminalJoin))
                .param("deviceId", deviceId).param("registeredAt", toOffsetDateTime(registeredAt))
                .param("tenantId", data.tenantId()).param("companyId", data.companyId())
                .param("establishmentId", data.establishmentId());
        if (data.terminalId() != null) query = query.param("terminalId", data.terminalId());
        query = bindNullable(query, "fingerprint", data.fingerprintHash());
        query = bindNullable(query, "thumbprint", data.certificateThumbprint());
        query = bindNullable(query, "agentVersion", data.agentVersion());
        return query.update() == 1;
    }

    @Override
    @Transactional
    public boolean updateDeviceStatus(UUID tenantId, UUID deviceId, String status) {
        return jdbc.sql("""
                        UPDATE sch_seguridad.dispositivo_tienda d SET estado = :status
                          FROM sch_admin.tenant t
                         WHERE t.id = d.tenant_id AND t.uuid_publico = :tenantId
                           AND d.uuid_publico = :deviceId
                        """)
                .param("status", status).param("tenantId", tenantId).param("deviceId", deviceId)
                .update() == 1;
    }

    private static JdbcClient.StatementSpec bindNullable(
            JdbcClient.StatementSpec query, String name, String value) {
        if (value == null || value.isBlank()) return query.param(name, null, Types.VARCHAR);
        return query.param(name, value.trim());
    }

    private static AssignmentView assignment(ResultSet rs) throws SQLException {
        return new AssignmentView(uuid(rs, "uuid_publico"), uuid(rs, "rol_uuid"),
                rs.getString("rol_codigo"), rs.getString("rol_nombre"), rs.getString("tipo_ambito"),
                uuid(rs, "empresa_uuid"), uuid(rs, "establecimiento_uuid"), uuid(rs, "almacen_uuid"),
                uuid(rs, "terminal_uuid"), instant(rs, "vigente_desde"), instant(rs, "vigente_hasta"),
                rs.getString("estado"), rs.getString("created_by"), instant(rs, "created_at"));
    }

    private static SessionView session(ResultSet rs) throws SQLException {
        return new SessionView(uuid(rs, "uuid_sesion"), uuid(rs, "usuario_uuid"), rs.getString("provider"),
                rs.getString("auth_method"), rs.getString("canal"), rs.getString("ip_origen"),
                rs.getString("user_agent"), uuid(rs, "dispositivo_ref"), instant(rs, "login_at"),
                instant(rs, "ultimo_uso_at"), instant(rs, "expira_at"), instant(rs, "logout_at"),
                instant(rs, "revocado_at"), rs.getString("motivo_revocacion"), rs.getString("estado"));
    }

    private static DeviceView device(ResultSet rs) throws SQLException {
        return new DeviceView(uuid(rs, "uuid_publico"), uuid(rs, "empresa_uuid"),
                uuid(rs, "establecimiento_uuid"), uuid(rs, "terminal_uuid"),
                rs.getString("device_fingerprint_hash"), rs.getString("certificado_thumbprint"),
                rs.getString("version_agente"), rs.getString("estado"),
                instant(rs, "registrado_at"), instant(rs, "ultimo_contacto_at"));
    }

    private static UUID uuid(ResultSet rs, String column) throws SQLException {
        return rs.getObject(column, UUID.class);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        var value = rs.getObject(column, OffsetDateTime.class);
        return value == null ? null : value.toInstant();
    }

    private static OffsetDateTime toOffsetDateTime(Instant value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }
}
