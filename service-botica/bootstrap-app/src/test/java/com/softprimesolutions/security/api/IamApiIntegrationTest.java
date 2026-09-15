package com.softprimesolutions.security.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.softprimesolutions.security.api.PasswordResetRequested;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RecordApplicationEvents
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class IamApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");
    private static final UUID COMPANY_ID = UUID.fromString("355a4f28-97d4-4492-9a72-32200cff87d6");
    private static final UUID ESTABLISHMENT_ID = UUID.fromString("6549f665-e596-4847-8048-f89a98d280e2");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private ApplicationEvents applicationEvents;

    @BeforeEach
    void prepareCanonicalSchemaDependencies() {
        resetCanonicalFixtures();
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'TEST', 'Tenant de prueba', 'tenant-de-prueba', 'test')
                        """)
                .param("tenantId", TENANT_ID)
                .update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.modulo_sistema (codigo, nombre, orden)
                        VALUES ('SEGURIDAD', 'Seguridad', 10)
                        """).update();
        seedOrganizationScope();
    }

    @Test
    void controlsStatusesAssignmentsEffectivePermissionsSessionsDevicesAndModules() throws Exception {
        var userId = UUID.randomUUID();
        var roleId = UUID.randomUUID();
        var assignmentId = UUID.randomUUID();
        var sessionId = UUID.randomUUID();

        var controlIdentidadId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.identidad
                            (uuid_publico, email, nombres, created_at)
                        VALUES (:identidadId, 'operador.control@example.test', 'Operador control', CURRENT_TIMESTAMP)
                        """).param("identidadId", controlIdentidadId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.membership
                            (uuid_publico, tenant_id, identidad_id, nombre_mostrar, requiere_cambio_credencial,
                             mfa_requerido, estado, created_at)
                        SELECT :userId, t.id, i.id, 'Operador control', FALSE, FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t, sch_seguridad.identidad i
                         WHERE t.uuid_publico = :tenantId AND i.uuid_publico = :identidadId
                        """).param("userId", userId).param("tenantId", TENANT_ID)
                .param("identidadId", controlIdentidadId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.rol
                            (uuid_publico, tenant_id, codigo, nombre, tipo_rol, es_sistema, estado, created_at)
                        SELECT :roleId, id, 'CONTROL', 'Control', 'GLOBAL', FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant WHERE uuid_publico = :tenantId
                        """).param("roleId", roleId).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.permiso
                            (modulo_id, codigo, recurso, accion, nombre, es_critico, estado)
                        SELECT id, 'seguridad.control.probar', 'CONTROL', 'PROBAR', 'Probar control', FALSE, 'ACTIVO'
                          FROM sch_seguridad.modulo_sistema WHERE codigo = 'SEGURIDAD'
                        """).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.rol_permiso
                            (tenant_id, rol_id, permiso_id, estado, granted_at, granted_by)
                        SELECT t.id, r.id, p.id, 'ACTIVO', CURRENT_TIMESTAMP, 'test'
                          FROM sch_admin.tenant t, sch_seguridad.rol r, sch_seguridad.permiso p
                         WHERE t.uuid_publico = :tenantId AND r.uuid_publico = :roleId
                           AND p.codigo = 'seguridad.control.probar'
                        """).param("tenantId", TENANT_ID).param("roleId", roleId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.usuario_rol_ambito
                            (uuid_publico, tenant_id, membership_id, rol_id, tipo_ambito,
                             vigente_desde, estado, created_by, created_at)
                        SELECT :assignmentId, t.id, m.id, r.id, 'GLOBAL', CURRENT_TIMESTAMP,
                               'ACTIVO', 'test', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t, sch_seguridad.membership m, sch_seguridad.rol r
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           AND r.uuid_publico = :roleId
                        """).param("assignmentId", assignmentId).param("tenantId", TENANT_ID)
                .param("userId", userId).param("roleId", roleId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.sesion_usuario
                            (uuid_sesion, tenant_id, membership_id, provider, canal, login_at, estado)
                        SELECT :sessionId, t.id, m.id, 'oidc', 'WEB', CURRENT_TIMESTAMP, 'ACTIVA'
                          FROM sch_admin.tenant t, sch_seguridad.membership m
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                        """).param("sessionId", sessionId).param("tenantId", TENANT_ID)
                .param("userId", userId).update();

        mockMvc.perform(post("/api/v1/usuarios/{userId}/identidades-externas", userId)
                        .with(controlAdmin()).with(csrf()).param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"oidc\",\"subject\":\"control-subject-1\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/usuarios/{userId}/identidades-externas", userId)
                        .with(controlAdmin()).with(csrf()).param("tenantId", TENANT_ID.toString())
                        .param("provider", "oidc").param("subject", "control-subject-1"))
                .andExpect(status().isConflict());
        mockMvc.perform(post("/api/v1/usuarios/{userId}/identidades-externas", userId)
                        .with(controlAdmin()).with(csrf()).param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"provider\":\"saml\",\"subject\":\"control-subject-2\"}"))
                .andExpect(status().isCreated());
        mockMvc.perform(delete("/api/v1/usuarios/{userId}/identidades-externas", userId)
                        .with(controlAdmin()).with(csrf()).param("tenantId", TENANT_ID.toString())
                        .param("provider", "oidc").param("subject", "control-subject-1"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/usuarios/{userId}/identidades-externas", userId)
                        .with(controlAdmin()).param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].provider").value("saml"));

        mockMvc.perform(get("/api/v1/modulos").with(controlAdmin()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("SEGURIDAD"));
        mockMvc.perform(get("/api/v1/usuarios/{userId}/asignaciones-rol", userId)
                        .with(controlAdmin()).param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].id").value(assignmentId.toString()));
        mockMvc.perform(get("/api/v1/usuarios/{userId}/permisos-efectivos", userId)
                        .with(controlAdmin()).param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0]").value("seguridad.control.probar"));
        mockMvc.perform(patch("/api/v1/roles/{roleId}/estado", roleId).with(controlAdmin()).with(csrf())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/roles/{roleId}/estado", roleId).with(controlAdmin()).with(csrf())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"ACTIVO\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(patch("/api/v1/usuarios/{userId}/estado", userId).with(controlAdmin()).with(csrf())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"BLOQUEADO\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/sesiones").with(controlAdmin())
                        .param("tenantId", TENANT_ID.toString()).param("userId", userId.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("ACTIVA"));
        mockMvc.perform(post("/api/v1/sesiones/{sessionId}/revocacion", sessionId)
                        .with(controlAdmin()).with(csrf()).param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"reason\":\"cierre administrativo\"}"))
                .andExpect(status().isNoContent());

        var deviceResponse = mockMvc.perform(post("/api/v1/dispositivos").with(controlAdmin()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"tenantId":"%s","companyId":"%s","establishmentId":"%s",
                                 "fingerprintHash":"hash-test","agentVersion":"1.0.0"}
                                """.formatted(TENANT_ID, COMPANY_ID, ESTABLISHMENT_ID)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDIENTE"))
                .andReturn().getResponse().getContentAsString();
        String deviceId = JsonPath.read(deviceResponse, "$.id");
        mockMvc.perform(patch("/api/v1/dispositivos/{deviceId}/estado", deviceId)
                        .with(controlAdmin()).with(csrf()).param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"CONFIABLE\"}"))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/dispositivos").with(controlAdmin())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$[0].status").value("CONFIABLE"));

        mockMvc.perform(delete("/api/v1/usuarios/{userId}/asignaciones-rol/{assignmentId}", userId, assignmentId)
                        .with(controlAdmin()).with(csrf()).param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isNoContent());
        mockMvc.perform(get("/api/v1/usuarios/{userId}/permisos-efectivos", userId)
                        .with(controlAdmin()).param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk()).andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void authenticatesLocallyWithJwtRefreshPasswordRecoveryAndLogout() throws Exception {
        var userId = UUID.randomUUID();
        var identidadId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.identidad
                            (uuid_publico, email, username, nombres, created_at)
                        VALUES (:identidadId, 'local.admin@example.test', 'local.admin',
                                'Administrador local', CURRENT_TIMESTAMP)
                        """).param("identidadId", identidadId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.membership
                            (uuid_publico, tenant_id, identidad_id, nombre_mostrar,
                             requiere_cambio_credencial, mfa_requerido, estado, created_at)
                        SELECT :userId, t.id, i.id, 'Administrador local', FALSE, FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t, sch_seguridad.identidad i
                         WHERE t.uuid_publico = :tenantId AND i.uuid_publico = :identidadId
                        """).param("userId", userId).param("tenantId", TENANT_ID)
                .param("identidadId", identidadId).update();

        mockMvc.perform(post("/api/v1/usuarios/{userId}/credencial-local", userId)
                        .with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                                {"tenantId":"%s","password":"InitialPass!2026","requireChange":false}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"login":"local.admin","password":"InitialPass!2026","channel":"POS"}
                        """))
                .andExpect(status().isUnauthorized());

        var login = login("InitialPass!2026");
        String firstRefresh = JsonPath.read(login, "$.refreshToken");
        var refreshed = mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(firstRefresh)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        String refreshedAccess = JsonPath.read(refreshed, "$.accessToken");
        mockMvc.perform(post("/api/v1/auth/refresh").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"refreshToken\":\"%s\"}".formatted(firstRefresh)))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(post("/api/v1/auth/password/change")
                        .header("Authorization", "Bearer " + refreshedAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"InitialPass!2026\",\"newPassword\":\"ChangedPass!2026\"}"))
                .andExpect(status().isUnauthorized());

        var secondLogin = login("InitialPass!2026");
        String secondAccess = JsonPath.read(secondLogin, "$.accessToken");
        mockMvc.perform(post("/api/v1/auth/password/change")
                        .header("Authorization", "Bearer " + secondAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"InitialPass!2026\",\"newPassword\":\"ChangedPass!2026\"}"))
                .andExpect(status().isNoContent());
        loginExpecting("InitialPass!2026", 401);
        loginExpecting("ChangedPass!2026", 200);

        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"login":"local.admin@example.test"}
                                """))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/v1/auth/password/forgot")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"login":"cuenta.inexistente@example.test"}
                                """))
                .andExpect(status().isAccepted());
        var resetEvent = applicationEvents.stream(PasswordResetRequested.class).reduce((first, last) -> last)
                .orElseThrow();
        mockMvc.perform(post("/api/v1/auth/password/reset")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"resetToken":"%s","newPassword":"RecoveredPass!2026"}
                                """.formatted(resetEvent.resetToken())))
                .andExpect(status().isNoContent());
        loginExpecting("ChangedPass!2026", 401);

        var recoveredLogin = login("RecoveredPass!2026");
        String recoveredAccess = JsonPath.read(recoveredLogin, "$.accessToken");
        mockMvc.perform(post("/api/v1/auth/logout")
                        .header("Authorization", "Bearer " + recoveredAccess))
                .andExpect(status().isNoContent());
        mockMvc.perform(post("/api/v1/auth/password/change")
                        .header("Authorization", "Bearer " + recoveredAccess)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"RecoveredPass!2026\",\"newPassword\":\"FinalPass!2026\"}"))
                .andExpect(status().isUnauthorized());
        for (int attempt = 0; attempt < 5; attempt++) {
            loginExpecting("IncorrectPass!2026", 401);
        }
        loginExpecting("RecoveredPass!2026", 401);
    }

    @Test
    void managesUsersRolesPermissionsAndScopedAssignments() throws Exception {
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.permiso
                            (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
                        SELECT id, 'seguridad.usuarios.consultar', 'USUARIO', 'CONSULTAR',
                               'Consultar usuarios', 'Consulta perfiles IAM', FALSE, 'ACTIVO'
                          FROM sch_seguridad.modulo_sistema
                         WHERE codigo = 'SEGURIDAD'
                        """)
                .update();

        var roleResponse = mockMvc.perform(post("/api/v1/roles")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"%s",
                                  "code":"ADMIN_LOCAL",
                                  "name":"Administrador local",
                                  "roleType":"ESTABLECIMIENTO",
                                  "systemRole":false
                                }
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.permissionCodes").isEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String roleId = JsonPath.read(roleResponse, "$.id");

        mockMvc.perform(put("/api/v1/roles/{roleId}/permissions", roleId)
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"permissionCodes":["seguridad.usuarios.consultar"]}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.permissionCodes[0]").value("seguridad.usuarios.consultar"));

        var userResponse = mockMvc.perform(post("/api/v1/usuarios")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"%s",
                                  "email":"admin@example.test",
                                  "displayName":"Ada Lovelace"
                                }
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("ACTIVO"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        String userId = JsonPath.read(userResponse, "$.id");

        mockMvc.perform(post("/api/v1/usuarios/{userId}/role-assignments", userId)
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","roleId":"%s","scopeType":"GLOBAL"}
                                """.formatted(TENANT_ID, roleId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scopeType").value("GLOBAL"));

        mockMvc.perform(get("/api/v1/usuarios")
                        .with(admin())
                        .param("tenantId", TENANT_ID.toString())
                        .param("search", "ada"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].email").value("admin@example.test"));

        mockMvc.perform(get("/api/v1/roles")
                        .with(admin())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].permissionCodes[0]")
                        .value("seguridad.usuarios.consultar"));

        mockMvc.perform(get("/api/v1/permisos").with(admin()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("seguridad.usuarios.consultar"));
    }

    @Test
    void grantsEffectiveAuthoritiesFromEstablishmentScopedRoleAssignment() throws Exception {
        var userId = UUID.randomUUID();
        var roleId = UUID.randomUUID();

        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.permiso
                            (modulo_id, codigo, recurso, accion, nombre, descripcion, es_critico, estado)
                        SELECT id, 'seguridad.sesiones.consultar', 'SESION', 'CONSULTAR',
                               'Consultar sesiones', 'Permite consultar las sesiones locales de acceso.',
                               TRUE, 'ACTIVO'
                          FROM sch_seguridad.modulo_sistema WHERE codigo = 'SEGURIDAD'
                        """).update();
        var cajeroIdentidadId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.identidad
                            (uuid_publico, email, username, nombres, created_at)
                        VALUES (:identidadId, 'tienda.cajero@example.test', 'tienda.cajero',
                                'Cajero de tienda', CURRENT_TIMESTAMP)
                        """).param("identidadId", cajeroIdentidadId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.membership
                            (uuid_publico, tenant_id, identidad_id, nombre_mostrar,
                             requiere_cambio_credencial, mfa_requerido, estado, created_at)
                        SELECT :userId, t.id, i.id, 'Cajero de tienda', FALSE, FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t, sch_seguridad.identidad i
                         WHERE t.uuid_publico = :tenantId AND i.uuid_publico = :identidadId
                        """).param("userId", userId).param("tenantId", TENANT_ID)
                .param("identidadId", cajeroIdentidadId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.rol
                            (uuid_publico, tenant_id, codigo, nombre, tipo_rol, es_sistema, estado, created_at)
                        SELECT :roleId, id, 'TIENDA', 'Tienda', 'ESTABLECIMIENTO', FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant WHERE uuid_publico = :tenantId
                        """).param("roleId", roleId).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.rol_permiso
                            (tenant_id, rol_id, permiso_id, estado, granted_at, granted_by)
                        SELECT t.id, r.id, p.id, 'ACTIVO', CURRENT_TIMESTAMP, 'test'
                          FROM sch_admin.tenant t, sch_seguridad.rol r, sch_seguridad.permiso p
                         WHERE t.uuid_publico = :tenantId AND r.uuid_publico = :roleId
                           AND p.codigo = 'seguridad.sesiones.consultar'
                        """).param("tenantId", TENANT_ID).param("roleId", roleId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.usuario_rol_ambito
                            (uuid_publico, tenant_id, membership_id, rol_id, tipo_ambito,
                             empresa_id, establecimiento_id, vigente_desde, estado, created_by, created_at)
                        SELECT :assignmentId, t.id, m.id, r.id, 'ESTABLECIMIENTO', e.id, est.id,
                               CURRENT_TIMESTAMP, 'ACTIVO', 'test', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t, sch_seguridad.membership m, sch_seguridad.rol r,
                               sch_organizacion.empresa_operadora e, sch_organizacion.establecimiento_farmaceutico est
                         WHERE t.uuid_publico = :tenantId AND m.uuid_publico = :userId
                           AND r.uuid_publico = :roleId AND e.uuid_publico = :companyId
                           AND est.uuid_publico = :establishmentId
                        """).param("assignmentId", UUID.randomUUID()).param("tenantId", TENANT_ID)
                .param("userId", userId).param("roleId", roleId)
                .param("companyId", COMPANY_ID).param("establishmentId", ESTABLISHMENT_ID).update();

        mockMvc.perform(post("/api/v1/usuarios/{userId}/credencial-local", userId)
                        .with(admin()).contentType(MediaType.APPLICATION_JSON).content("""
                                {"tenantId":"%s","password":"TiendaPass!2026","requireChange":false}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        var login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content("""
                        {"login":"tienda.cajero","password":"TiendaPass!2026","channel":"WEB"}
                        """))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String accessToken = JsonPath.read(login, "$.accessToken");

        mockMvc.perform(get("/api/v1/sesiones")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk());
    }

    @Test
    void deniesIamAdministrationWithoutTheRequiredPermission() throws Exception {
        mockMvc.perform(get("/api/v1/roles")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer"))
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsProblemDetailsForInvalidHttpInput() throws Exception {
        mockMvc.perform(post("/api/v1/usuarios")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"bad","displayName":""}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
    }

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return SecurityMockMvcRequestPostProcessors.user("admin").authorities(
                new SimpleGrantedAuthority("seguridad.usuarios.gestionar"),
                new SimpleGrantedAuthority("seguridad.usuarios.consultar"),
                new SimpleGrantedAuthority("seguridad.roles.gestionar"),
                new SimpleGrantedAuthority("seguridad.roles.consultar"),
                new SimpleGrantedAuthority("seguridad.roles.asignar"),
                new SimpleGrantedAuthority("seguridad.permisos.asignar"),
                new SimpleGrantedAuthority("seguridad.permisos.consultar"),
                new SimpleGrantedAuthority("seguridad.credenciales.gestionar"));
    }

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor controlAdmin() {
        return SecurityMockMvcRequestPostProcessors.user("security-admin").authorities(
                new SimpleGrantedAuthority("seguridad.usuarios.gestionar"),
                new SimpleGrantedAuthority("seguridad.usuarios.consultar"),
                new SimpleGrantedAuthority("seguridad.identidades.gestionar"),
                new SimpleGrantedAuthority("seguridad.roles.consultar"),
                new SimpleGrantedAuthority("seguridad.roles.gestionar"),
                new SimpleGrantedAuthority("seguridad.roles.asignar"),
                new SimpleGrantedAuthority("seguridad.permisos.consultar"),
                new SimpleGrantedAuthority("seguridad.modulos.consultar"),
                new SimpleGrantedAuthority("seguridad.sesiones.consultar"),
                new SimpleGrantedAuthority("seguridad.sesiones.revocar"),
                new SimpleGrantedAuthority("seguridad.dispositivos.consultar"),
                new SimpleGrantedAuthority("seguridad.dispositivos.gestionar"));
    }

    private void resetCanonicalFixtures() {
        jdbcClient.sql("DELETE FROM sch_app.menu_navegacion").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.identidad_externa").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.usuario_rol_ambito").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.rol_permiso").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.token_refresh").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.token_recuperacion_password").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.sesion_usuario").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.credencial_local").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.dispositivo_tienda").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.permiso").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.rol").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.membership").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.identidad").update();
        jdbcClient.sql("DELETE FROM sch_organizacion.terminal_pos").update();
        jdbcClient.sql("DELETE FROM sch_organizacion.almacen").update();
        jdbcClient.sql("DELETE FROM sch_organizacion.establecimiento_farmaceutico").update();
        jdbcClient.sql("DELETE FROM sch_organizacion.empresa_operadora").update();
        jdbcClient.sql("DELETE FROM sch_seguridad.modulo_sistema").update();
        jdbcClient.sql("DELETE FROM sch_admin.tenant").update();
    }

    private void seedOrganizationScope() {
        jdbcClient.sql("""
                        INSERT INTO sch_organizacion.empresa_operadora
                            (uuid_publico, tenant_id, ruc, razon_social, created_by)
                        SELECT :companyId, id, '20123456789', 'Empresa de prueba', 'test'
                          FROM sch_admin.tenant WHERE uuid_publico = :tenantId
                        """).param("companyId", COMPANY_ID).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_organizacion.establecimiento_farmaceutico
                            (uuid_publico, tenant_id, empresa_id, codigo, nombre,
                             tipo_establecimiento, created_by)
                        SELECT :establishmentId, t.id, e.id, 'EST-TEST', 'Establecimiento de prueba',
                               'FARMACIA', 'test'
                          FROM sch_admin.tenant t, sch_organizacion.empresa_operadora e
                         WHERE t.uuid_publico = :tenantId AND e.uuid_publico = :companyId
                        """).param("establishmentId", ESTABLISHMENT_ID).param("tenantId", TENANT_ID)
                .param("companyId", COMPANY_ID).update();
    }

    private String login(String password) throws Exception {
        return mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"login":"local.admin","password":"%s","channel":"WEB"}
                                """.formatted(password)))
                .andExpect(status().isOk())
                .andExpect(header().string("Cache-Control", "no-store"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andReturn().getResponse().getContentAsString();
    }

    private void loginExpecting(String password, int expectedStatus) throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"login":"local.admin","password":"%s","channel":"WEB"}
                                """.formatted(password)))
                .andExpect(status().is(expectedStatus));
    }
}
