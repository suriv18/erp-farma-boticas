package com.softprimesolutions.clientes.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import com.softprimesolutions.testsupport.RealLogin;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class ClientesApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("3a7c9e21-5b4d-4f60-8c13-9e2a7b6d1f45");
    private static final UUID OTHER_TENANT_ID = UUID.fromString("c41f8d07-2e6a-4b93-a5d0-7f1e3c9b8a62");
    private static final String BASE = "/api/v1/clientes/clientes";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private RealLogin realLogin;
    private String admin;
    private String sinPermisos;

    @BeforeEach
    void createTenantsAndLogIn() throws Exception {
        realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        admin = provision(TENANT_ID, "CLITEST", "clientes.admin", "^clientes\\.");
        sinPermisos = realLogin.login(TENANT_ID, "clientes.sin.permisos", null).bearer();
    }

    private String provision(UUID tenantId, String codigo, String username, String permissions) throws Exception {
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, :codigo, 'Tenant clientes', LOWER(:codigo), 'test')
                        """).param("tenantId", tenantId).param("codigo", codigo).update();
        return realLogin.login(tenantId, username, permissions).bearer();
    }

    private UUID register(String bearer, String body) throws Exception {
        var response = mockMvc.perform(post(BASE).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(response, "$.id"));
    }

    private static final String ANA = """
            {"tipoCliente":"NATURAL","tipoDocumento":"1","numeroDocumento":"12345678",
             "nombres":"Ana","apellidos":"Pérez","email":"ana@correo.pe"}
            """;

    @Test
    void registersAClientAndReadsItBackByIdAndInTheList() throws Exception {
        var id = register(admin, ANA);

        mockMvc.perform(get(BASE + "/{id}", id).header("Authorization", admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.tipoCliente").value("NATURAL"))
                .andExpect(jsonPath("$.numeroDocumento").value("12345678"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andExpect(jsonPath("$.email").value("ana@correo.pe"));
        mockMvc.perform(get(BASE).header("Authorization", admin).param("search", "pérez"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(id.toString()));
        mockMvc.perform(get(BASE).header("Authorization", admin).param("search", "inexistente"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void returnsTheExistingClientWhenTheDocumentIsAlreadyRegistered() throws Exception {
        var id = register(admin, ANA);

        mockMvc.perform(post(BASE).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                        .content(ANA.replace("Ana", "Otra")))
                .andExpect(status().isOk())
                .andExpect(header().doesNotExist("Location"))
                .andExpect(jsonPath("$.id").value(id.toString()))
                .andExpect(jsonPath("$.nombres").value("Ana"));
        mockMvc.perform(get(BASE).header("Authorization", admin))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void registersAnAnonymousClientWithoutDocument() throws Exception {
        register(admin, """
                {"tipoCliente":"SIN_IDENTIFICAR"}
                """);
        register(admin, """
                {"tipoCliente":"SIN_IDENTIFICAR"}
                """);

        mockMvc.perform(get(BASE).header("Authorization", admin))
                .andExpect(jsonPath("$.totalElements").value(2));
    }

    @Test
    void registersAJuridicalClientWithARuc() throws Exception {
        register(admin, """
                {"tipoCliente":"JURIDICO","tipoDocumento":"6","numeroDocumento":"20123456789",
                 "razonSocial":"Botica Central SAC"}
                """);
    }

    @Test
    void rejectsInvalidData() throws Exception {
        for (var body : new String[] {
                """
                {"tipoCliente":"NATURAL","tipoDocumento":"1","numeroDocumento":"123","nombres":"Ana","apellidos":"Pérez"}
                """,
                """
                {"tipoCliente":"VIP"}
                """,
                """
                {"tipoCliente":"NATURAL","tipoDocumento":"Z","numeroDocumento":"123","nombres":"Ana","apellidos":"Pérez"}
                """,
                """
                {"tipoDocumento":"1"}
                """}) {
            mockMvc.perform(post(BASE).header("Authorization", admin).contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isBadRequest());
        }
    }

    @Test
    void rejectsInvalidPagination() throws Exception {
        mockMvc.perform(get(BASE).header("Authorization", admin).param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void requiresAnExistingClientAndTheClientsPermissions() throws Exception {
        mockMvc.perform(get(BASE + "/{id}", UUID.randomUUID()).header("Authorization", admin))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(BASE).header("Authorization", sinPermisos)).andExpect(status().isForbidden());
        mockMvc.perform(post(BASE).header("Authorization", sinPermisos).contentType(MediaType.APPLICATION_JSON)
                        .content(ANA))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(BASE)).andExpect(status().isUnauthorized());
    }

    @Test
    void doesNotShowAClientOfAnotherTenant() throws Exception {
        var id = register(admin, ANA);
        var otro = provision(OTHER_TENANT_ID, "CLIOTRO", "clientes.otro", "^clientes\\.");

        mockMvc.perform(get(BASE + "/{id}", id).header("Authorization", otro)).andExpect(status().isNotFound());
        mockMvc.perform(get(BASE).header("Authorization", otro))
                .andExpect(jsonPath("$.totalElements").value(0));
        register(otro, ANA);
    }
}
