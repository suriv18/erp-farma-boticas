package com.softprimesolutions.testsupport;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.web.servlet.MockMvc;

public final class RealLogin {

    public static final String PASSWORD = "RealLogin2026!Valid";

    public record Session(UUID userId, String bearer) {
    }

    private final JdbcClient jdbcClient;
    private final MockMvc mockMvc;
    private final PasswordHashPort passwordHash;

    public RealLogin(JdbcClient jdbcClient, MockMvc mockMvc, PasswordHashPort passwordHash) {
        this.jdbcClient = jdbcClient;
        this.mockMvc = mockMvc;
        this.passwordHash = passwordHash;
    }

    public Session login(UUID tenantId, String username, String permissionRegex) throws Exception {
        var userId = UUID.randomUUID();
        var identidadId = UUID.randomUUID();
        var roleCode = "IT_" + username.toUpperCase().replaceAll("[^A-Z0-9]", "_");
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.identidad (uuid_publico, email, username, nombres, created_at)
                        VALUES (:identidadId, :email, :username, :username, CURRENT_TIMESTAMP)
                        """).param("identidadId", identidadId).param("email", username + "@example.test")
                .param("username", username).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.membership
                            (uuid_publico, tenant_id, identidad_id, nombre_mostrar, requiere_cambio_credencial,
                             mfa_requerido, estado, created_at)
                        SELECT :userId, t.id, i.id, :username, FALSE, FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t, sch_seguridad.identidad i
                         WHERE t.uuid_publico = :tenantId AND i.uuid_publico = :identidadId
                        """).param("userId", userId).param("username", username).param("tenantId", tenantId)
                .param("identidadId", identidadId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.credencial_local (membership_id, password_hash, requiere_cambio)
                        SELECT id, :hash, FALSE FROM sch_seguridad.membership WHERE uuid_publico = :userId
                        """).param("hash", passwordHash.encode(PASSWORD)).param("userId", userId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.rol
                            (uuid_publico, tenant_id, codigo, nombre, tipo_rol, es_sistema, estado, created_at)
                        SELECT :roleId, id, :roleCode, :roleCode, 'GLOBAL', FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant WHERE uuid_publico = :tenantId
                        """).param("roleId", UUID.randomUUID()).param("roleCode", roleCode)
                .param("tenantId", tenantId).update();
        if (permissionRegex != null) {
            jdbcClient.sql("""
                            INSERT INTO sch_seguridad.rol_permiso
                                (tenant_id, rol_id, permiso_id, estado, granted_at, granted_by)
                            SELECT t.id, r.id, p.id, 'ACTIVO', CURRENT_TIMESTAMP, 'test'
                              FROM sch_admin.tenant t
                              JOIN sch_seguridad.rol r ON r.tenant_id = t.id AND r.codigo = :roleCode
                              JOIN sch_seguridad.permiso p ON p.codigo ~ :permissions AND p.es_activo = '1'
                             WHERE t.uuid_publico = :tenantId
                            """).param("roleCode", roleCode).param("permissions", permissionRegex)
                    .param("tenantId", tenantId).update();
        }
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.usuario_rol_ambito
                            (uuid_publico, tenant_id, membership_id, rol_id, tipo_ambito,
                             vigente_desde, estado, created_by, created_at)
                        SELECT :assignmentId, t.id, m.id, r.id, 'GLOBAL', CURRENT_TIMESTAMP,
                               'ACTIVO', 'test', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t
                          JOIN sch_seguridad.membership m ON m.tenant_id = t.id AND m.uuid_publico = :userId
                          JOIN sch_seguridad.rol r ON r.tenant_id = t.id AND r.codigo = :roleCode
                         WHERE t.uuid_publico = :tenantId
                        """).param("assignmentId", UUID.randomUUID()).param("roleCode", roleCode)
                .param("tenantId", tenantId).param("userId", userId).update();

        var response = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"%s","password":"%s","channel":"WEB"}
                                """.formatted(username, PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return new Session(userId, "Bearer " + JsonPath.read(response, "$.accessToken"));
    }
}
