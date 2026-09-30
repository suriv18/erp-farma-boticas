package com.softprimesolutions.organizacion.api;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.dao.DataIntegrityViolationException;
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
class OrganizacionApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("0f6d4c2e-3b1a-4c8e-9a51-7d2b6e4f1a90");
    private static final String PASSWORD = "OrgAdmin2026!Valid";
    private static final String BASE = "/api/v1/organizacion";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    private String accessToken;

    @BeforeEach
    void createTenantWithAnOrganizationAdministratorAndLogIn() throws Exception {
        var userId = UUID.randomUUID();
        var identidadId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'ORGTEST', 'Tenant organizacion', 'tenant-organizacion', 'test')
                        """).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.identidad (uuid_publico, email, username, nombres, created_at)
                        VALUES (:identidadId, 'org.admin@example.test', 'org.admin', 'Admin organizacion',
                                CURRENT_TIMESTAMP)
                        """).param("identidadId", identidadId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.membership
                            (uuid_publico, tenant_id, identidad_id, nombre_mostrar, requiere_cambio_credencial,
                             mfa_requerido, estado, created_at)
                        SELECT :userId, t.id, i.id, 'Admin organizacion', FALSE, FALSE, 'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t, sch_seguridad.identidad i
                         WHERE t.uuid_publico = :tenantId AND i.uuid_publico = :identidadId
                        """).param("userId", userId).param("tenantId", TENANT_ID)
                .param("identidadId", identidadId).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.rol
                            (uuid_publico, tenant_id, codigo, nombre, tipo_rol, es_sistema, estado, created_at)
                        SELECT :roleId, id, 'ORG_ADMIN', 'Administrador de organizacion', 'GLOBAL', FALSE,
                               'ACTIVO', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant WHERE uuid_publico = :tenantId
                        """).param("roleId", UUID.randomUUID()).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.rol_permiso
                            (tenant_id, rol_id, permiso_id, estado, granted_at, granted_by)
                        SELECT t.id, r.id, p.id, 'ACTIVO', CURRENT_TIMESTAMP, 'test'
                          FROM sch_admin.tenant t
                          JOIN sch_seguridad.rol r ON r.tenant_id = t.id AND r.codigo = 'ORG_ADMIN'
                          JOIN sch_seguridad.permiso p ON p.codigo LIKE 'organizacion.%' AND p.es_activo = '1'
                         WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).update();
        jdbcClient.sql("""
                        INSERT INTO sch_seguridad.usuario_rol_ambito
                            (uuid_publico, tenant_id, membership_id, rol_id, tipo_ambito,
                             vigente_desde, estado, created_by, created_at)
                        SELECT :assignmentId, t.id, m.id, r.id, 'GLOBAL', CURRENT_TIMESTAMP,
                               'ACTIVO', 'test', CURRENT_TIMESTAMP
                          FROM sch_admin.tenant t
                          JOIN sch_seguridad.membership m ON m.tenant_id = t.id AND m.uuid_publico = :userId
                          JOIN sch_seguridad.rol r ON r.tenant_id = t.id AND r.codigo = 'ORG_ADMIN'
                         WHERE t.uuid_publico = :tenantId
                        """).param("assignmentId", UUID.randomUUID()).param("tenantId", TENANT_ID)
                .param("userId", userId).update();

        mockMvc.perform(post("/api/v1/usuarios/{userId}/credencial-local", userId)
                        .with(credentialAdmin()).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","password":"%s","requireChange":false}
                                """.formatted(TENANT_ID, PASSWORD)))
                .andExpect(status().isNoContent());
        var login = mockMvc.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"login":"org.admin","password":"%s","channel":"WEB"}
                                """.formatted(PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        accessToken = JsonPath.read(login, "$.accessToken");
    }

    @Test
    void buildsTheWholeCorporateStructureAndReadsItBackFromTheToken() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var establecimientoId = createEstablecimiento(empresaId, "EST001", "DIG001");
        var almacenId = createAlmacen(establecimientoId);
        var terminalId = createTerminal(establecimientoId);

        mockMvc.perform(get(BASE + "/empresas").header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(empresaId.toString()))
                .andExpect(jsonPath("$.items[0].ruc").value("20123456786"));
        mockMvc.perform(get(BASE + "/establecimientos").header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).param("empresaId", empresaId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].id").value(establecimientoId.toString()));
        mockMvc.perform(get(BASE + "/almacenes/{id}", almacenId).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(true));
        mockMvc.perform(get(BASE + "/terminales-pos/{id}", terminalId).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ipEquipo").value("10.0.0.15"));

        mockMvc.perform(get("/api/v1/estructura-corporativa").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.asOf").exists())
                .andExpect(jsonPath("$.companies[0].id").value(empresaId.toString()))
                .andExpect(jsonPath("$.companies[0].legalName").value("Boticas Integracion SAC"))
                .andExpect(jsonPath("$.companies[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.companies[0].establishments[0].id").value(establecimientoId.toString()))
                .andExpect(jsonPath("$.companies[0].establishments[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.companies[0].establishments[0].timeZone").value("America/Lima"))
                .andExpect(jsonPath("$.companies[0].establishments[0].warehouses[0].id")
                        .value(almacenId.toString()))
                .andExpect(jsonPath("$.companies[0].establishments[0].warehouses[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.companies[0].establishments[0].cashRegisters[0].id")
                        .value(terminalId.toString()))
                .andExpect(jsonPath("$.companies[0].establishments[0].cashRegisters[0].status").value("ACTIVE"));
    }

    @Test
    void updatesAndDeactivatesWhatWasCreated() throws Exception {
        var empresaId = createEmpresa("20123456794");
        var establecimientoId = createEstablecimiento(empresaId, "EST010", null);
        var almacenId = createAlmacen(establecimientoId);

        mockMvc.perform(put(BASE + "/empresas/{id}", empresaId).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"razonSocial":"Boticas Renombradas SAC","monedaFuncional":"PEN",
                                 "zonaHoraria":"America/Lima","permiteVentaOnline":true}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.razonSocial").value("Boticas Renombradas SAC"))
                .andExpect(jsonPath("$.permiteVentaOnline").value(true));
        mockMvc.perform(put(BASE + "/almacenes/{id}", almacenId).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Almacen Inactivo","tipo":"GENERAL","permiteLotes":true,
                                 "permiteVencimiento":true,"permiteVenta":true,"permiteDespacho":true,
                                 "controlTemperatura":false,"activo":false}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activo").value(false));

        mockMvc.perform(get("/api/v1/estructura-corporativa").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies[0].establishments[0].warehouses[0].status").value("INACTIVE"));
    }

    @Test
    void rejectsDuplicatesAndUnknownParents() throws Exception {
        var empresaId = createEmpresa("20123456786");
        createEstablecimiento(empresaId, "EST001", "DIG001");

        mockMvc.perform(post(BASE + "/empresas").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content(empresaJson("20123456786")))
                .andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/establecimientos").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(establecimientoJson(empresaId, "EST001", null)))
                .andExpect(status().isConflict());
        mockMvc.perform(post(BASE + "/establecimientos").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(establecimientoJson(UUID.randomUUID(), "EST002", null)))
                .andExpect(status().isNotFound());
        mockMvc.perform(post(BASE + "/empresas").header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantId\":\"%s\",\"ruc\":\"123\"}".formatted(TENANT_ID)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deniesAccessWithoutTheRequiredPermissionOrToken() throws Exception {
        mockMvc.perform(post(BASE + "/empresas").with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-permisos"))
                        .contentType(MediaType.APPLICATION_JSON).content(empresaJson("20123456786")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/estructura-corporativa")
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-permisos")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get("/api/v1/estructura-corporativa")).andExpect(status().isUnauthorized());
    }

    @Test
    void changesTheStatusOfAnEmpresaAndAnEstablecimiento() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var establecimientoId = createEstablecimiento(empresaId, "EST001", null);

        patchEstado("/empresas/{id}/estado", empresaId, "SUSPENDIDO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("SUSPENDIDO"));
        patchEstado("/establecimientos/{id}/estado", establecimientoId, "CLAUSURADO")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoOperativo").value("CLAUSURADO"));

        mockMvc.perform(get("/api/v1/estructura-corporativa").header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.companies[0].status").value("INACTIVE"))
                .andExpect(jsonPath("$.companies[0].establishments[0].status").value("INACTIVE"));
    }

    @Test
    void rejectsInvalidStatusUnknownResourcesAndMissingPermission() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var establecimientoId = createEstablecimiento(empresaId, "EST001", null);

        patchEstado("/empresas/{id}/estado", empresaId, "FOO").andExpect(status().isBadRequest());
        patchEstado("/empresas/{id}/estado", empresaId, "").andExpect(status().isBadRequest());
        patchEstado("/establecimientos/{id}/estado", establecimientoId, "BLOQUEADO")
                .andExpect(status().isBadRequest());
        patchEstado("/empresas/{id}/estado", UUID.randomUUID(), "SUSPENDIDO").andExpect(status().isNotFound());
        patchEstado("/establecimientos/{id}/estado", UUID.randomUUID(), "SUSPENDIDO")
                .andExpect(status().isNotFound());
        mockMvc.perform(patch(BASE + "/empresas/{id}/estado", empresaId).with(csrf())
                        .with(SecurityMockMvcRequestPostProcessors.user("sin-permisos"))
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"estado\":\"SUSPENDIDO\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectsAnEmpresaWhoseRucHasAnInvalidCheckDigit() throws Exception {
        mockMvc.perform(post(BASE + "/empresas").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(empresaJson("20123456789")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("El RUC no es válido: el dígito verificador no coincide."));
    }

    @Test
    void rejectsRepeatedSeriesWithinAnEmpresaButAllowsThemInAnotherOne() throws Exception {
        var empresaA = createEmpresa("20123456786");
        var sedeUno = createEstablecimiento(empresaA, "EST001", null);
        var sedeDos = createEstablecimiento(empresaA, "EST002", null);
        var empresaB = createEmpresa("20123456794");
        var sedeTres = createEstablecimiento(empresaB, "EST003", null);

        postTerminal(sedeUno, "POS001", "B001", "F001").andExpect(status().isCreated());

        postTerminal(sedeDos, "POS002", "B001", "F002").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La serie B001 ya está asignada a otra caja de esta empresa."));
        postTerminal(sedeDos, "POS002", "B002", "F001").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La serie F001 ya está asignada a otra caja de esta empresa."));
        postTerminal(sedeTres, "POS001", "B001", "F001").andExpect(status().isCreated());
    }

    @Test
    void rejectsEditingATerminalToASerieUsedByAnotherTerminalOfTheEmpresa() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var sedeId = createEstablecimiento(empresaId, "EST001", null);
        postTerminal(sedeId, "POS001", "B001", "F001").andExpect(status().isCreated());
        var segundaCaja = UUID.fromString(JsonPath.read(
                postTerminal(sedeId, "POS002", "B002", "F002").andExpect(status().isCreated())
                        .andReturn().getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(put(BASE + "/terminales-pos/{id}", segundaCaja).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Caja 2","serieBoletaDefecto":"B001","serieFacturaDefecto":"F002",
                                 "storeEdgeHabilitado":false,"estado":"ACTIVO"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("La serie B001 ya está asignada a otra caja de esta empresa."));
        mockMvc.perform(put(BASE + "/terminales-pos/{id}", segundaCaja).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Caja 2","serieBoletaDefecto":"B002","serieFacturaDefecto":"F002",
                                 "storeEdgeHabilitado":false,"estado":"ACTIVO"}
                                """))
                .andExpect(status().isOk());
    }

    @Test
    void blocksNewEstablecimientosWhenTheEmpresaIsNotOperational() throws Exception {
        var empresaId = createEmpresa("20123456786");
        patchEstado("/empresas/{id}/estado", empresaId, "SUSPENDIDO").andExpect(status().isOk());

        mockMvc.perform(post(BASE + "/establecimientos").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(establecimientoJson(empresaId, "EST001", null)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "La empresa no está operativa (suspendida o bloqueada); no admite establecimientos nuevos."));

        patchEstado("/empresas/{id}/estado", empresaId, "ACTIVO").andExpect(status().isOk());
        createEstablecimiento(empresaId, "EST001", null);
    }

    @Test
    void blocksNewAlmacenesAndTerminalesWhenTheEstablecimientoIsClosedButAllowsThemInRemodelacion()
            throws Exception {
        var empresaId = createEmpresa("20123456786");
        var sedeId = createEstablecimiento(empresaId, "EST001", null);

        patchEstado("/establecimientos/{id}/estado", sedeId, "CLAUSURADO").andExpect(status().isOk());
        postAlmacen(sedeId, "ALM001", "GENERAL", false, "null", "null").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El establecimiento no está operativo (suspendido o clausurado); no admite almacenes nuevos."));
        postTerminal(sedeId, "POS001", "B001", "F001").andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(
                        "El establecimiento no está operativo (suspendido o clausurado); no admite terminales POS nuevos."));

        patchEstado("/establecimientos/{id}/estado", sedeId, "REMODELACION").andExpect(status().isOk());
        postAlmacen(sedeId, "ALM001", "GENERAL", false, "null", "null").andExpect(status().isCreated());
        postTerminal(sedeId, "POS001", "B001", "F001").andExpect(status().isCreated());
    }

    @Test
    void rejectsAlmacenesWithIncoherentTemperatures() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var sedeId = createEstablecimiento(empresaId, "EST001", null);

        postAlmacen(sedeId, "ALM001", "REFRIGERADO", false, "2", "8").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Un almacén refrigerado debe controlar temperatura."));
        postAlmacen(sedeId, "ALM002", "GENERAL", true, "null", "8").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "Indica la temperatura mínima y máxima cuando el almacén controla temperatura."));
        postAlmacen(sedeId, "ALM003", "REFRIGERADO", true, "8", "2").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La temperatura mínima no puede ser mayor que la máxima."));
        postAlmacen(sedeId, "ALM004", "REFRIGERADO", true, "2", "8").andExpect(status().isCreated());
    }

    @Test
    void rejectsTerminalesWithLowercaseSeriesOnCreateAndUpdate() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var sedeId = createEstablecimiento(empresaId, "EST001", null);

        postTerminal(sedeId, "POS001", "b001", "F001").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "La serie de boleta debe iniciar con B y tener 4 caracteres alfanuméricos en mayúscula."));

        var terminalId = createTerminal(sedeId);
        mockMvc.perform(put(BASE + "/terminales-pos/{id}", terminalId).header("Authorization", bearer())
                        .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"nombre":"Caja 1","serieBoletaDefecto":"b001","serieFacturaDefecto":"F001",
                                 "storeEdgeHabilitado":false,"estado":"ACTIVO"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value(
                        "La serie de boleta debe iniciar con B y tener 4 caracteres alfanuméricos en mayúscula."));
    }

    @Test
    void theDatabaseRejectsARepeatedBoletaSerieWithTheIndexNameTheAdapterMapsOn() throws Exception {
        var empresaId = createEmpresa("20123456786");
        var sedeId = createEstablecimiento(empresaId, "EST001", null);
        var terminalId = createTerminal(sedeId);

        assertThatThrownBy(() -> jdbcClient.sql("""
                        INSERT INTO sch_organizacion.terminal_pos
                            (tenant_id, empresa_id, establecimiento_id, codigo, nombre, serie_boleta_defecto)
                        SELECT tenant_id, empresa_id, establecimiento_id, 'POS999', 'Caja duplicada', serie_boleta_defecto
                          FROM sch_organizacion.terminal_pos WHERE uuid_publico = :terminalId
                        """).param("terminalId", terminalId).update())
                .isInstanceOf(DataIntegrityViolationException.class)
                .hasMessageContaining("uk_terminal_pos_serie_boleta");
    }

    private org.springframework.test.web.servlet.ResultActions patchEstado(String path, UUID id, String estado)
            throws Exception {
        return mockMvc.perform(patch(BASE + path, id).header("Authorization", bearer())
                .param("tenantId", TENANT_ID.toString()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"estado\":\"" + estado + "\"}"));
    }

    private UUID createEmpresa(String ruc) throws Exception {
        return created(post(BASE + "/empresas").content(empresaJson(ruc)));
    }

    private UUID createEstablecimiento(UUID empresaId, String codigo, String digemid) throws Exception {
        return created(post(BASE + "/establecimientos").content(establecimientoJson(empresaId, codigo, digemid)));
    }

    private UUID createAlmacen(UUID establecimientoId) throws Exception {
        return created(post(BASE + "/almacenes").content("""
                {"tenantId":"%s","establecimientoId":"%s","codigo":"ALM001","nombre":"Almacen Central",
                 "tipo":"GENERAL","permiteLotes":true,"permiteVencimiento":true,"permiteVenta":true,
                 "permiteDespacho":true,"controlTemperatura":false}
                """.formatted(TENANT_ID, establecimientoId)));
    }

    private UUID createTerminal(UUID establecimientoId) throws Exception {
        return created(post(BASE + "/terminales-pos").content("""
                {"tenantId":"%s","establecimientoId":"%s","codigo":"POS001","nombre":"Caja 1",
                 "serieBoletaDefecto":"B001","serieFacturaDefecto":"F001","numeroSerieEquipo":"SN-001",
                 "hostname":"caja-1","ipEquipo":"10.0.0.15","impresoraCodigo":"IMP01","storeEdgeHabilitado":true}
                """.formatted(TENANT_ID, establecimientoId)));
    }

    private org.springframework.test.web.servlet.ResultActions postTerminal(
            UUID establecimientoId, String codigo, String serieBoleta, String serieFactura) throws Exception {
        return mockMvc.perform(post(BASE + "/terminales-pos").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"tenantId":"%s","establecimientoId":"%s","codigo":"%s","nombre":"Caja",
                         "serieBoletaDefecto":"%s","serieFacturaDefecto":"%s","storeEdgeHabilitado":false}
                        """.formatted(TENANT_ID, establecimientoId, codigo, serieBoleta, serieFactura)));
    }

    private org.springframework.test.web.servlet.ResultActions postAlmacen(
            UUID establecimientoId, String codigo, String tipo, boolean controlTemperatura, String min, String max)
            throws Exception {
        return mockMvc.perform(post(BASE + "/almacenes").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON).content("""
                        {"tenantId":"%s","establecimientoId":"%s","codigo":"%s","nombre":"Almacen",
                         "tipo":"%s","permiteLotes":true,"permiteVencimiento":true,"permiteVenta":true,
                         "permiteDespacho":true,"controlTemperatura":%s,"temperaturaMinC":%s,
                         "temperaturaMaxC":%s}
                        """.formatted(TENANT_ID, establecimientoId, codigo, tipo, controlTemperatura, min, max)));
    }

    private UUID created(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder request)
            throws Exception {
        var body = mockMvc.perform(request.header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private String empresaJson(String ruc) {
        return """
                {"tenantId":"%s","ruc":"%s","razonSocial":"Boticas Integracion SAC","nombreComercial":"Boticas",
                 "direccionFiscal":"Av. Principal 100","ubigeoFiscal":"150101","monedaFuncional":"PEN",
                 "zonaHoraria":"America/Lima","permiteVentaOnline":false}
                """.formatted(TENANT_ID, ruc);
    }

    private String establecimientoJson(UUID empresaId, String codigo, String digemid) {
        var digemidField = digemid == null ? "" : ",\"codigoDigemid\":\"" + digemid + "\"";
        return """
                {"tenantId":"%s","empresaId":"%s","codigo":"%s","nombre":"Botica Central",
                 "tipoEstablecimiento":"BOTICA","codigoAnexoSunat":"0001","direccion":"Av. Principal 100",
                 "ubigeo":"150101","esPrincipal":true,"permiteVentaOnline":false,"permiteDelivery":false,
                 "perfilOperacion":"ONLINE","zonaHoraria":"America/Lima"%s}
                """.formatted(TENANT_ID, empresaId, codigo, digemidField);
    }

    private String bearer() {
        return "Bearer " + accessToken;
    }

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor credentialAdmin() {
        return SecurityMockMvcRequestPostProcessors.user("credential-admin").authorities(
                new SimpleGrantedAuthority("seguridad.credenciales.gestionar"),
                new SimpleGrantedAuthority("seguridad.usuarios.gestionar"));
    }
}
