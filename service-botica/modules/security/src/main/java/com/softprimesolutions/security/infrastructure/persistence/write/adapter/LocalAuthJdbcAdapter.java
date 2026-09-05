package com.softprimesolutions.security.infrastructure.persistence.write.adapter;

import com.softprimesolutions.security.application.port.out.LocalAuthStorePort;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
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
public class LocalAuthJdbcAdapter implements LocalAuthStorePort {

    private final JdbcClient jdbc;

    public LocalAuthJdbcAdapter(JdbcClient jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalAccount> findAccountByLogin(UUID tenantId, String login) {
        return jdbc.sql(accountSelect() + """
                         WHERE t.uuid_publico = :tenantId
                           AND (LOWER(CAST(u.username AS VARCHAR)) = LOWER(:login)
                             OR LOWER(CAST(u.email AS VARCHAR)) = LOWER(:login))
                        """)
                .param("tenantId", tenantId).param("login", login)
                .query((rs, row) -> account(rs)).optional();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<LocalAccount> findAccountByUser(UUID tenantId, UUID userId) {
        return jdbc.sql(accountSelect() + """
                         WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .query((rs, row) -> account(rs)).optional();
    }

    @Override
    @Transactional
    public ProvisionOutcome provisionCredential(
            UUID tenantId, UUID userId, String passwordHash, boolean requireChange, Instant at) {
        var userInternalId = findUserInternalId(tenantId, userId);
        if (userInternalId.isEmpty()) return ProvisionOutcome.USER_NOT_FOUND;
        var updated = jdbc.sql("""
                        UPDATE sch_seguridad.credencial_local
                           SET password_hash = :passwordHash, intentos_fallidos = 0, bloqueado_hasta = NULL,
                               requiere_cambio = :requireChange, estado = 'ACTIVA',
                               password_changed_at = :at, updated_at = :at
                         WHERE usuario_id = :userId
                        """)
                .param("passwordHash", passwordHash).param("requireChange", requireChange)
                .param("at", toOffsetDateTime(at)).param("userId", userInternalId.get()).update();
        if (updated == 1) {
            updateUserPasswordChangeRequirement(tenantId, userId, requireChange, at);
            revokeAllUserSessions(tenantId, userId, "CREDENCIAL_REEMPLAZADA", at);
            return ProvisionOutcome.UPDATED;
        }
        try {
            jdbc.sql("""
                            INSERT INTO sch_seguridad.credencial_local
                                (usuario_id, password_hash, requiere_cambio, estado,
                                 password_changed_at, created_at)
                            VALUES (:userId, :passwordHash, :requireChange, 'ACTIVA', :at, :at)
                            """)
                    .param("userId", userInternalId.get()).param("passwordHash", passwordHash)
                    .param("requireChange", requireChange)
                    .param("at", toOffsetDateTime(at)).update();
            updateUserPasswordChangeRequirement(tenantId, userId, requireChange, at);
            return ProvisionOutcome.CREATED;
        } catch (DataIntegrityViolationException concurrentInsert) {
            return provisionCredential(tenantId, userId, passwordHash, requireChange, at);
        }
    }

    @Override
    @Transactional
    public void recordLoginFailure(
            UUID tenantId, UUID userId, int maximumAttempts, Instant lockedUntil, Instant at) {
        jdbc.sql("""
                        UPDATE sch_seguridad.credencial_local c
                           SET intentos_fallidos = c.intentos_fallidos + 1,
                               bloqueado_hasta = CASE
                                   WHEN c.intentos_fallidos + 1 >= :maximumAttempts THEN :lockedUntil
                                   ELSE c.bloqueado_hasta END,
                               updated_at = :at
                         WHERE c.usuario_id = (
                               SELECT u.id FROM sch_seguridad.usuario u
                               JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                                WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId)
                        """)
                .param("maximumAttempts", maximumAttempts)
                .param("lockedUntil", toOffsetDateTime(lockedUntil))
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
    }

    @Override
    @Transactional
    public void recordLoginSuccess(UUID tenantId, UUID userId, Instant at) {
        jdbc.sql("""
                        UPDATE sch_seguridad.credencial_local c
                           SET intentos_fallidos = 0, bloqueado_hasta = NULL, updated_at = :at
                         WHERE c.usuario_id = (
                               SELECT u.id FROM sch_seguridad.usuario u
                               JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                                WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId)
                        """)
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
        jdbc.sql("""
                        UPDATE sch_seguridad.usuario u SET ultimo_login_at = :at, updated_at = :at
                         WHERE u.uuid_publico = :userId AND u.tenant_id = (
                               SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantId)
                        """)
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
    }

    @Override
    @Transactional
    public boolean createSessionAndRefresh(SessionData session, RefreshTokenData refresh) {
        var inserted = jdbc.sql("""
                        INSERT INTO sch_seguridad.sesion_usuario
                            (uuid_sesion, tenant_id, usuario_id, provider, auth_method, canal,
                             ip_origen, user_agent, dispositivo_ref, login_at, ultimo_uso_at,
                             expira_at, estado)
                        SELECT :sessionId, t.id, u.id, 'local', 'PASSWORD', :channel,
                               :ipAddress, :userAgent, :deviceId, :loginAt, :loginAt,
                               :expiresAt, 'ACTIVA'
                          FROM sch_seguridad.usuario u
                          JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                         WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId
                           AND u.estado = 'ACTIVO'
                        """)
                .param("sessionId", session.sessionId()).param("channel", session.channel())
                .param("loginAt", toOffsetDateTime(session.loginAt()))
                .param("expiresAt", toOffsetDateTime(session.expiresAt()))
                .param("tenantId", session.tenantId()).param("userId", session.userId())
                .param("ipAddress", session.ipAddress(), Types.OTHER)
                .param("userAgent", session.userAgent(), Types.VARCHAR)
                .param("deviceId", session.deviceId(), Types.OTHER).update();
        if (inserted != 1) return false;
        return insertRefresh(refresh) == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<StoredRefreshToken> findRefreshToken(String tokenHash) {
        return jdbc.sql("""
                        SELECT t.uuid_publico AS tenant_uuid, u.uuid_publico AS user_uuid,
                               r.sesion_uuid, r.familia_uuid, r.expira_at, r.usado_at, r.revocado_at
                          FROM sch_seguridad.token_refresh r
                          JOIN sch_farmacia.tenant t ON t.id = r.tenant_id
                          JOIN sch_seguridad.usuario u ON u.id = r.usuario_id AND u.tenant_id = r.tenant_id
                         WHERE r.token_hash = :tokenHash
                        """)
                .param("tokenHash", tokenHash)
                .query((rs, row) -> new StoredRefreshToken(
                        uuid(rs, "tenant_uuid"), uuid(rs, "user_uuid"), uuid(rs, "sesion_uuid"),
                        uuid(rs, "familia_uuid"), instant(rs, "expira_at"), instant(rs, "usado_at"),
                        instant(rs, "revocado_at"))).optional();
    }

    @Override
    @Transactional
    public boolean rotateRefreshToken(String currentHash, RefreshTokenData replacement, Instant at) {
        var updated = jdbc.sql("""
                        UPDATE sch_seguridad.token_refresh
                           SET usado_at = :at, reemplazado_por = :replacementId
                         WHERE token_hash = :currentHash AND usado_at IS NULL AND revocado_at IS NULL
                           AND expira_at > :at
                        """)
                .param("at", toOffsetDateTime(at)).param("replacementId", replacement.id())
                .param("currentHash", currentHash).update();
        return updated == 1 && insertRefresh(replacement) == 1;
    }

    @Override
    @Transactional
    public boolean revokeSession(UUID tenantId, UUID userId, UUID sessionId, String reason, Instant at) {
        var updated = jdbc.sql("""
                        UPDATE sch_seguridad.sesion_usuario s
                           SET estado = 'REVOCADA', revocado_at = :at, motivo_revocacion = :reason
                         WHERE s.uuid_sesion = :sessionId AND s.estado = 'ACTIVA'
                           AND s.usuario_id = (
                               SELECT u.id FROM sch_seguridad.usuario u
                               JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                                WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId)
                        """)
                .param("at", toOffsetDateTime(at)).param("reason", reason).param("sessionId", sessionId)
                .param("tenantId", tenantId).param("userId", userId).update();
        jdbc.sql("""
                        UPDATE sch_seguridad.token_refresh SET revocado_at = :at
                         WHERE sesion_uuid = :sessionId AND revocado_at IS NULL
                        """)
                .param("at", toOffsetDateTime(at)).param("sessionId", sessionId).update();
        return updated == 1;
    }

    @Override
    @Transactional
    public void createPasswordReset(
            UUID tenantId, UUID userId, String tokenHash, Instant expiresAt, Instant at) {
        var userInternalId = findUserInternalId(tenantId, userId).orElseThrow();
        var tenantInternalId = findTenantInternalId(tenantId).orElseThrow();
        jdbc.sql("""
                        UPDATE sch_seguridad.token_recuperacion_password
                           SET revocado_at = :at
                         WHERE tenant_id = :tenantId AND usuario_id = :userId
                           AND consumido_at IS NULL AND revocado_at IS NULL
                        """)
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantInternalId).param("userId", userInternalId).update();
        jdbc.sql("""
                        INSERT INTO sch_seguridad.token_recuperacion_password
                            (tenant_id, usuario_id, token_hash, expira_at, created_at)
                        VALUES (:tenantId, :userId, :tokenHash, :expiresAt, :at)
                        """)
                .param("tenantId", tenantInternalId).param("userId", userInternalId)
                .param("tokenHash", tokenHash)
                .param("expiresAt", toOffsetDateTime(expiresAt))
                .param("at", toOffsetDateTime(at)).update();
    }

    @Override
    @Transactional
    public boolean resetPassword(String tokenHash, String newPasswordHash, Instant at) {
        var target = jdbc.sql("""
                        SELECT t.uuid_publico AS tenant_uuid, u.uuid_publico AS user_uuid
                          FROM sch_seguridad.token_recuperacion_password p
                          JOIN sch_farmacia.tenant t ON t.id = p.tenant_id
                          JOIN sch_seguridad.usuario u ON u.id = p.usuario_id AND u.tenant_id = p.tenant_id
                         WHERE p.token_hash = :tokenHash AND p.consumido_at IS NULL
                           AND p.revocado_at IS NULL AND p.expira_at > :at
                        """)
                .param("tokenHash", tokenHash).param("at", toOffsetDateTime(at))
                .query((rs, row) -> new UserReference(uuid(rs, "tenant_uuid"), uuid(rs, "user_uuid")))
                .optional();
        if (target.isEmpty()) return false;
        var consumed = jdbc.sql("""
                        UPDATE sch_seguridad.token_recuperacion_password SET consumido_at = :at
                         WHERE token_hash = :tokenHash AND consumido_at IS NULL
                           AND revocado_at IS NULL AND expira_at > :at
                        """)
                .param("at", toOffsetDateTime(at)).param("tokenHash", tokenHash).update();
        if (consumed != 1) return false;
        return replacePasswordAndRevoke(target.get(), newPasswordHash, at, "PASSWORD_RECUPERADA");
    }

    @Override
    @Transactional
    public boolean changePassword(UUID tenantId, UUID userId, String newPasswordHash, Instant at) {
        return replacePasswordAndRevoke(
                new UserReference(tenantId, userId), newPasswordHash, at, "PASSWORD_CAMBIADA");
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isSessionActive(UUID tenantId, UUID userId, UUID sessionId, Instant at) {
        return jdbc.sql("""
                        SELECT COUNT(*)
                          FROM sch_seguridad.sesion_usuario s
                          JOIN sch_seguridad.usuario u ON u.id = s.usuario_id AND u.tenant_id = s.tenant_id
                          JOIN sch_farmacia.tenant t ON t.id = s.tenant_id
                         WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId
                           AND s.uuid_sesion = :sessionId AND s.estado = 'ACTIVA'
                           AND u.estado = 'ACTIVO' AND u.mfa_requerido = FALSE
                           AND EXISTS (
                               SELECT 1 FROM sch_seguridad.credencial_local c
                                WHERE c.usuario_id = u.id AND c.estado = 'ACTIVA'
                           )
                           AND (s.expira_at IS NULL OR s.expira_at > :at)
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .param("sessionId", sessionId).param("at", toOffsetDateTime(at))
                .query(Long.class).single() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isTrustedDevice(UUID tenantId, UUID deviceId) {
        return jdbc.sql("""
                        SELECT COUNT(*) FROM sch_seguridad.dispositivo_tienda d
                          JOIN sch_farmacia.tenant t ON t.id = d.tenant_id
                         WHERE t.uuid_publico = :tenantId AND d.uuid_publico = :deviceId
                           AND d.estado = 'CONFIABLE'
                        """)
                .param("tenantId", tenantId).param("deviceId", deviceId).query(Long.class).single() == 1;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<String> findEffectivePermissions(UUID tenantId, UUID userId, Instant at) {
        return new LinkedHashSet<>(jdbc.sql("""
                        SELECT DISTINCT p.codigo
                          FROM sch_seguridad.usuario_rol_ambito a
                          JOIN sch_seguridad.usuario u ON u.id = a.usuario_id AND u.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol r ON r.id = a.rol_id AND r.tenant_id = a.tenant_id
                          JOIN sch_seguridad.rol_permiso rp ON rp.rol_id = r.id AND rp.tenant_id = r.tenant_id
                          JOIN sch_seguridad.permiso p ON p.id = rp.permiso_id
                          JOIN sch_farmacia.tenant t ON t.id = a.tenant_id
                         WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId
                           AND u.estado = 'ACTIVO' AND r.estado = 'ACTIVO'
                           AND a.estado = 'ACTIVO' AND rp.estado = 'ACTIVO' AND p.estado = 'ACTIVO'
                           AND a.vigente_desde <= :at
                           AND (a.vigente_hasta IS NULL OR a.vigente_hasta >= :at)
                         ORDER BY p.codigo
                        """)
                .param("tenantId", tenantId).param("userId", userId)
                .param("at", toOffsetDateTime(at))
                .query(String.class).list());
    }

    private int insertRefresh(RefreshTokenData value) {
        return jdbc.sql("""
                        INSERT INTO sch_seguridad.token_refresh
                            (uuid_publico, tenant_id, usuario_id, sesion_uuid, familia_uuid,
                             token_hash, expira_at, created_at)
                        SELECT :id, t.id, u.id, :sessionId, :familyId, :tokenHash, :expiresAt, :createdAt
                          FROM sch_seguridad.usuario u
                          JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                         WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId
                        """)
                .param("id", value.id()).param("sessionId", value.sessionId())
                .param("familyId", value.familyId()).param("tokenHash", value.tokenHash())
                .param("expiresAt", toOffsetDateTime(value.expiresAt()))
                .param("createdAt", toOffsetDateTime(value.createdAt()))
                .param("tenantId", value.tenantId()).param("userId", value.userId()).update();
    }

    private boolean replacePasswordAndRevoke(
            UserReference user, String newPasswordHash, Instant at, String reason) {
        var updated = jdbc.sql("""
                        UPDATE sch_seguridad.credencial_local c
                           SET password_hash = :passwordHash, intentos_fallidos = 0,
                               bloqueado_hasta = NULL, requiere_cambio = FALSE, estado = 'ACTIVA',
                               password_changed_at = :at, updated_at = :at
                         WHERE c.usuario_id = (
                               SELECT u.id FROM sch_seguridad.usuario u
                               JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                                WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId)
                        """)
                .param("passwordHash", newPasswordHash).param("at", toOffsetDateTime(at))
                .param("tenantId", user.tenantId()).param("userId", user.userId()).update();
        if (updated != 1) return false;
        updateUserPasswordChangeRequirement(user.tenantId(), user.userId(), false, at);
        revokeAllUserSessions(user.tenantId(), user.userId(), reason, at);
        return true;
    }

    private void revokeAllUserSessions(UUID tenantId, UUID userId, String reason, Instant at) {
        jdbc.sql("""
                        UPDATE sch_seguridad.sesion_usuario s
                           SET estado = 'REVOCADA', revocado_at = :at, motivo_revocacion = :reason
                         WHERE s.estado = 'ACTIVA' AND s.usuario_id = (
                               SELECT u.id FROM sch_seguridad.usuario u
                               JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                                WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId)
                        """)
                .param("at", toOffsetDateTime(at)).param("reason", reason)
                .param("tenantId", tenantId).param("userId", userId).update();
        jdbc.sql("""
                        UPDATE sch_seguridad.token_refresh r SET revocado_at = :at
                         WHERE r.revocado_at IS NULL AND r.usuario_id = (
                               SELECT u.id FROM sch_seguridad.usuario u
                               JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                                WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId)
                        """)
                .param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
    }

    private void updateUserPasswordChangeRequirement(
            UUID tenantId, UUID userId, boolean requireChange, Instant at) {
        jdbc.sql("""
                        UPDATE sch_seguridad.usuario u
                           SET requiere_cambio_credencial = :requireChange, updated_at = :at
                         WHERE u.uuid_publico = :userId AND u.tenant_id = (
                               SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantId)
                        """)
                .param("requireChange", requireChange).param("at", toOffsetDateTime(at))
                .param("tenantId", tenantId).param("userId", userId).update();
    }

    private Optional<Long> findUserInternalId(UUID tenantId, UUID userId) {
        return jdbc.sql("""
                        SELECT u.id FROM sch_seguridad.usuario u
                          JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                         WHERE t.uuid_publico = :tenantId AND u.uuid_publico = :userId
                        """).param("tenantId", tenantId).param("userId", userId)
                .query(Long.class).optional();
    }

    private Optional<Long> findTenantInternalId(UUID tenantId) {
        return jdbc.sql("SELECT id FROM sch_farmacia.tenant WHERE uuid_publico = :tenantId")
                .param("tenantId", tenantId).query(Long.class).optional();
    }

    private static String accountSelect() {
        return """
                SELECT t.uuid_publico AS tenant_uuid, u.uuid_publico AS user_uuid,
                       CAST(u.username AS VARCHAR) AS username, CAST(u.email AS VARCHAR) AS email,
                       u.nombre_mostrar, u.estado AS user_status, c.estado AS credential_status,
                       c.password_hash, c.intentos_fallidos, c.bloqueado_hasta,
                       (c.requiere_cambio OR u.requiere_cambio_credencial) AS requiere_cambio,
                       u.mfa_requerido
                  FROM sch_seguridad.usuario u
                  JOIN sch_farmacia.tenant t ON t.id = u.tenant_id
                  JOIN sch_seguridad.credencial_local c ON c.usuario_id = u.id
                """;
    }

    private static LocalAccount account(ResultSet rs) throws SQLException {
        return new LocalAccount(uuid(rs, "tenant_uuid"), uuid(rs, "user_uuid"), rs.getString("username"),
                rs.getString("email"), rs.getString("nombre_mostrar"), rs.getString("user_status"),
                rs.getString("credential_status"), rs.getString("password_hash"),
                rs.getInt("intentos_fallidos"), instant(rs, "bloqueado_hasta"),
                rs.getBoolean("requiere_cambio"), rs.getBoolean("mfa_requerido"));
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

    private record UserReference(UUID tenantId, UUID userId) {
    }
}
