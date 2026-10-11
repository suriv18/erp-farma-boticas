package com.softprimesolutions.security.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class LocalAuthLoginWithoutTenantIdTest {

    private static final UUID TENANT_ID = UUID.fromString("7c61d383-861c-4c6b-9749-9096ce4a74cf");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

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
    }

    @Test
    void createsAUserAndLogsInWithoutSendingTenantId() throws Exception {
        var userResponse = mockMvc.perform(post("/api/v1/usuarios")
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tenantId":"%s",
                                  "email":"sinssoyaml@example.test",
                                  "username":"sinssoyaml",
                                  "displayName":"Sin SSO"
                                }
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String userId = JsonPath.read(userResponse, "$.id");

        mockMvc.perform(post("/api/v1/usuarios/{userId}/credencial-local", userId)
                        .with(admin())
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","password":"SinSso2026!Valid","requireChange":false}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"sinssoyaml@example.test","password":"SinSso2026!Valid"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").exists())
                .andExpect(jsonPath("$.userId").exists());
    }

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor admin() {
        return SecurityMockMvcRequestPostProcessors.user("admin").authorities(
                new SimpleGrantedAuthority("seguridad.usuarios.gestionar"),
                new SimpleGrantedAuthority("seguridad.usuarios.consultar"),
                new SimpleGrantedAuthority("seguridad.credenciales.gestionar"));
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
}
