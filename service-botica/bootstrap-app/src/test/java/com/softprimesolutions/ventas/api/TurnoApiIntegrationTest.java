package com.softprimesolutions.ventas.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class TurnoApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("b7e1d2c3-4a5f-4e6d-9c8b-1a2b3c4d5e6f");
    private static final String ORG = "/api/v1/organizacion";
    private static final String TURNOS = "/api/v1/ventas/turnos";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private String bearer;
    private String noPermissions;
    private UUID userId;
    private UUID terminalId;

    @BeforeEach
    void createTenantAdministratorAndATerminal() throws Exception {
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'VENTEST', 'Tenant ventas', 'tenant-ventas', 'test')
                        """).param("tenantId", TENANT_ID).update();
        var realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        var session = realLogin.login(TENANT_ID, "ven.admin", "^(organizacion|ventas)\\.");
        userId = session.userId();
        bearer = session.bearer();
        noPermissions = realLogin.login(TENANT_ID, "sin.permisos", null).bearer();

        var empresaId = created(post(ORG + "/empresas").content("""
                {"tenantId":"%s","ruc":"20123456786","razonSocial":"Boticas Ventas SAC",
                 "monedaFuncional":"PEN","zonaHoraria":"America/Lima","permiteVentaOnline":false}
                """.formatted(TENANT_ID)));
        var establecimientoId = created(post(ORG + "/establecimientos").content("""
                {"tenantId":"%s","empresaId":"%s","codigo":"EST001","nombre":"Botica Central",
                 "tipoEstablecimiento":"BOTICA","codigoAnexoSunat":"0001","esPrincipal":true,
                 "permiteVentaOnline":false,"permiteDelivery":false,"perfilOperacion":"ONLINE",
                 "zonaHoraria":"America/Lima"}
                """.formatted(TENANT_ID, empresaId)));
        terminalId = created(post(ORG + "/terminales-pos").content("""
                {"establecimientoId":"%s","codigo":"POS001","nombre":"Caja 1",
                 "serieBoletaDefecto":"B001","serieFacturaDefecto":"F001","storeEdgeHabilitado":false}
                """.formatted(establecimientoId)));
    }

    @Test
    void opensConsultsAndClosesATurnoComputingTheCashDifference() throws Exception {
        var apertura = abrir("50.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.terminalId").value(terminalId.toString()))
                .andExpect(jsonPath("$.cajeroId").value(userId.toString()))
                .andExpect(jsonPath("$.fondoInicial").value(50.0))
                .andExpect(jsonPath("$.totalVentasSistema").value(0.0))
                .andExpect(jsonPath("$.totalSistema").value(50.0))
                .andReturn().getResponse().getContentAsString();
        var turnoId = UUID.fromString(JsonPath.read(apertura, "$.id"));

        mockMvc.perform(get(TURNOS + "/actual").header("Authorization", bearer)
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(turnoId.toString()));

        mockMvc.perform(post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDeclarado\":45.50,\"observacion\":\"Faltante\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"))
                .andExpect(jsonPath("$.totalVentasSistema").value(0.0))
                .andExpect(jsonPath("$.totalSistema").value(50.0))
                .andExpect(jsonPath("$.totalDeclarado").value(45.5))
                .andExpect(jsonPath("$.diferencia").value(-4.5))
                .andExpect(jsonPath("$.observacionCierre").value("Faltante"));

        mockMvc.perform(get(TURNOS + "/{id}", turnoId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("CERRADO"));
        mockMvc.perform(get(TURNOS + "/actual").header("Authorization", bearer)
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_NO_ENCONTRADO"));

        var fila = jdbcClient.sql("""
                        SELECT tc.created_by, tc.updated_by, m.uuid_publico AS cajero
                          FROM sch_venta.turno_caja tc
                          JOIN sch_seguridad.membership m ON m.id = tc.cajero_usuario_id
                         WHERE tc.uuid_publico = :turnoId
                        """).param("turnoId", turnoId).query().singleRow();
        assertThat(fila).containsEntry("created_by", userId.toString())
                .containsEntry("updated_by", userId.toString());
        assertThat(fila.get("cajero")).hasToString(userId.toString());
    }

    @Test
    void aSecondOpenTurnoOnTheSameTerminalIsAConflictUntilTheFirstIsClosed() throws Exception {
        var primero = abrir("10")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();

        abrir("10").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("VEN_TURNO_YA_ABIERTO"));

        mockMvc.perform(post(TURNOS + "/{id}/cierre", UUID.fromString(JsonPath.read(primero, "$.id")))
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"totalDeclarado\":10}"))
                .andExpect(status().isOk());
        abrir("20").andExpect(status().isCreated());
    }

    @Test
    void aClosedTurnoCannotBeClosedAgain() throws Exception {
        var apertura = abrir("10")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        var turnoId = UUID.fromString(JsonPath.read(apertura, "$.id"));
        var cierre = post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":10}");

        mockMvc.perform(cierre).andExpect(status().isOk());
        mockMvc.perform(cierre).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_ESTADO_INVALIDO"));
    }

    @Test
    void validatesTheRequestsAndTheirReferences() throws Exception {
        abrir("-1").andExpect(status().isBadRequest());
        mockMvc.perform(post(TURNOS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fondoInicial\":10}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(TURNOS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":\"%s\",\"fondoInicial\":10}".formatted(UUID.randomUUID())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_TERMINAL_NO_ENCONTRADA"));
        mockMvc.perform(get(TURNOS + "/{id}", UUID.randomUUID()).header("Authorization", bearer))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(TURNOS + "/{id}/cierre", UUID.randomUUID()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":10}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_NO_ENCONTRADO"));
    }

    @Test
    void aTerminalThatIsNotActiveCannotOpenATurno() throws Exception {
        jdbcClient.sql("UPDATE sch_organizacion.terminal_pos SET estado = 'BLOQUEADO' WHERE uuid_publico = :id")
                .param("id", terminalId).update();

        abrir("10").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_TERMINAL_NO_OPERABLE"));
    }

    @Test
    void deniesAccessWithoutThePermissionOrTheToken() throws Exception {
        mockMvc.perform(post(TURNOS).with(csrf()).header("Authorization", noPermissions)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":\"%s\",\"fondoInicial\":10}".formatted(terminalId)))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(TURNOS + "/actual").header("Authorization", noPermissions)
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(TURNOS + "/actual").param("terminalId", terminalId.toString()))
                .andExpect(status().isUnauthorized());
    }

    private ResultActions abrir(String fondo) throws Exception {
        return mockMvc.perform(post(TURNOS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content("{\"terminalId\":\"%s\",\"fondoInicial\":%s}".formatted(terminalId, fondo)));
    }

    private UUID created(MockHttpServletRequestBuilder request) throws Exception {
        var body = mockMvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
