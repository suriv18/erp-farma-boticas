package com.softprimesolutions.catalogo.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class CatalogoSoporteApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private RealLogin realLogin;
    private String noPermissions;
    private String soporteAdmin;
    private String principiosActivosAdmin;
    private String viewer;

    @BeforeEach
    void resetCanonicalFixturesAndLogIn() throws Exception {
        jdbcClient.sql("DELETE FROM sch_catalogo.principio_activo").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.clasificacion_controlada").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.condicion_venta").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.forma_farmaceutica").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.via_administracion").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.unidad_medida").update();
        var tenantId = UUID.fromString("c3a5e7f9-1b2d-4c6e-8a0b-4d6f8a0c2e4f");
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'CATSOP', 'Tenant soporte', 'tenant-soporte', 'test')
                        """).param("tenantId", tenantId).update();
        realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        soporteAdmin = realLogin.login(tenantId, "soporte.admin", "^catalogo\\.soporte\\.").bearer();
        principiosActivosAdmin = realLogin.login(
                tenantId, "principios.admin", "^catalogo\\.principios-activos\\.").bearer();
        viewer = realLogin.login(tenantId, "soporte.sin.permisos", null).bearer();
    }

    @Test
    void managesCondicionVentaCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/condiciones-venta")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"VENTA_LIBRE",
                                  "denominacion":"Venta libre",
                                  "requiereReceta":false,
                                  "requiereRetencion":false,
                                  "fuente":"DIGEMID",
                                  "versionFuente":"2026.1",
                                  "vigenteDesde":"2026-01-01"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("VENTA_LIBRE"))
                .andExpect(jsonPath("$.denominacion").value("Venta libre"))
                .andExpect(jsonPath("$.requiereReceta").value(false))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(put("/api/v1/catalogo/condiciones-venta/{codigo}", "VENTA_LIBRE")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"VENTA_LIBRE",
                                  "denominacion":"Venta libre sin receta",
                                  "requiereReceta":false,
                                  "requiereRetencion":true,
                                  "fuente":"DIGEMID",
                                  "versionFuente":"2026.2",
                                  "vigenteDesde":"2026-01-01"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Venta libre sin receta"))
                .andExpect(jsonPath("$.requiereRetencion").value(true));

        mockMvc.perform(patch("/api/v1/catalogo/condiciones-venta/{codigo}/estado", "VENTA_LIBRE")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/condiciones-venta").header("Authorization", soporteAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("VENTA_LIBRE"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }

    @Test
    void managesFormaFarmaceuticaCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/formas-farmaceuticas")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"TABLETA","denominacion":"Tableta","fuente":"DIGEMID"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("TABLETA"))
                .andExpect(jsonPath("$.denominacion").value("Tableta"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(put("/api/v1/catalogo/formas-farmaceuticas/{codigo}", "TABLETA")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"TABLETA","denominacion":"Tableta recubierta","fuente":"DIGEMID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Tableta recubierta"));

        mockMvc.perform(patch("/api/v1/catalogo/formas-farmaceuticas/{codigo}/estado", "TABLETA")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/formas-farmaceuticas").header("Authorization", soporteAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("TABLETA"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }

    @Test
    void managesViaAdministracionCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/vias-administracion")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"ORAL","denominacion":"Via oral","fuente":"DIGEMID"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("ORAL"))
                .andExpect(jsonPath("$.denominacion").value("Via oral"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(put("/api/v1/catalogo/vias-administracion/{codigo}", "ORAL")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"ORAL","denominacion":"Via oral actualizada","fuente":"DIGEMID"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Via oral actualizada"));

        mockMvc.perform(patch("/api/v1/catalogo/vias-administracion/{codigo}/estado", "ORAL")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/vias-administracion").header("Authorization", soporteAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("ORAL"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }

    @Test
    void managesUnidadMedidaCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/unidades-medida")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"MG",
                                  "denominacion":"Miligramo",
                                  "simbolo":"mg",
                                  "permiteDecimal":true,
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("MG"))
                .andExpect(jsonPath("$.simbolo").value("mg"))
                .andExpect(jsonPath("$.permiteDecimal").value(true))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        mockMvc.perform(put("/api/v1/catalogo/unidades-medida/{codigo}", "MG")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"MG",
                                  "denominacion":"Miligramo actualizado",
                                  "simbolo":"mg",
                                  "permiteDecimal":false,
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Miligramo actualizado"))
                .andExpect(jsonPath("$.permiteDecimal").value(false));

        mockMvc.perform(patch("/api/v1/catalogo/unidades-medida/{codigo}/estado", "MG")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/unidades-medida").header("Authorization", soporteAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("MG"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }

    @Test
    void managesClasificacionControladaCreateUpdateStatusAndList() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/clasificaciones-controladas")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"LISTA_II",
                                  "denominacion":"Lista II",
                                  "normaFuente":"D.S. 023-2001-SA",
                                  "requiereRecetaEspecial":true,
                                  "retieneReceta":true,
                                  "vigenciaRecetaDias":30
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("LISTA_II"))
                .andExpect(jsonPath("$.requiereRecetaEspecial").value(true))
                .andExpect(jsonPath("$.vigenciaRecetaDias").value(30))
                .andExpect(jsonPath("$.estado").value("ACTIVO"));

        // El PUT de este controller toma el codigo del BODY (no del path variable).
        mockMvc.perform(put("/api/v1/catalogo/clasificaciones-controladas/{codigo}", "LISTA_II")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"LISTA_II",
                                  "denominacion":"Lista II actualizada",
                                  "normaFuente":"D.S. 023-2001-SA",
                                  "requiereRecetaEspecial":true,
                                  "retieneReceta":false,
                                  "vigenciaRecetaDias":15
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Lista II actualizada"))
                .andExpect(jsonPath("$.retieneReceta").value(false))
                .andExpect(jsonPath("$.vigenciaRecetaDias").value(15));

        mockMvc.perform(patch("/api/v1/catalogo/clasificaciones-controladas/{codigo}/estado", "LISTA_II")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/clasificaciones-controladas").header("Authorization", soporteAdmin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].codigo").value("LISTA_II"))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));
    }

    @Test
    void managesPrincipioActivoCreateUpdateStatusListAndRejectsDuplicateDenomination() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/principios-activos")
                        .header("Authorization", principiosActivosAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoFuente":"PA-001",
                                  "denominacion":"Paracetamol",
                                  "nombreNormalizado":"PARACETAMOL",
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.denominacion").value("Paracetamol"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String principioActivoId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/principios-activos/{principioActivoId}", principioActivoId)
                        .header("Authorization", principiosActivosAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoFuente":"PA-001",
                                  "denominacion":"Paracetamol 500mg",
                                  "nombreNormalizado":"PARACETAMOL 500MG",
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Paracetamol 500mg"));

        mockMvc.perform(patch("/api/v1/catalogo/principios-activos/{principioActivoId}/estado", principioActivoId)
                        .header("Authorization", principiosActivosAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"status\":\"INACTIVO\"}"))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/principios-activos")
                        .header("Authorization", principiosActivosAdmin)
                        .param("texto", "Paracetamol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(principioActivoId))
                .andExpect(jsonPath("$[0].estado").value("INACTIVO"));

        // La denominacion "Paracetamol 500mg" ya existe (indice unico uk_principio_activo_nombre):
        // el handler de creacion traduce la violacion de integridad a un ApplicationError de conflicto.
        mockMvc.perform(post("/api/v1/catalogo/principios-activos")
                        .header("Authorization", principiosActivosAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigoFuente":"PA-002",
                                  "denominacion":"Paracetamol 500mg",
                                  "nombreNormalizado":"PARACETAMOL 500MG",
                                  "fuente":"DIGEMID"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CAT_PRINCIPIO_ACTIVO_DUPLICADO"));
    }

    @Test
    void deniesCatalogoSoporteAdministrationWithoutTheRequiredPermission() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/condiciones-venta")
                        .header("Authorization", viewer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"VENTA_LIBRE",
                                  "denominacion":"Venta libre",
                                  "requiereReceta":false,
                                  "requiereRetencion":false
                                }
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/catalogo/principios-activos")
                        .header("Authorization", viewer)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"denominacion":"Paracetamol"}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(get("/api/v1/catalogo/condiciones-venta")
                        .header("Authorization", viewer))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsProblemDetailsForInvalidHttpInput() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/condiciones-venta")
                        .header("Authorization", soporteAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "codigo":"",
                                  "denominacion":"X",
                                  "requiereReceta":false,
                                  "requiereRetencion":false
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/catalogo/principios-activos")
                        .header("Authorization", principiosActivosAdmin)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"denominacion":"P"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
    }
}
