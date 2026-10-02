package com.softprimesolutions.catalogo.api;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
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
import org.springframework.test.context.event.RecordApplicationEvents;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@RecordApplicationEvents
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class CatalogoComercialApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("2f8f3e3a-8c1b-4f6d-9a3e-2c9a2e6a9a11");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private RealLogin realLogin;
    private String noPermissions;
    private String gestor;
    private String consultor;
    private String viewer;
    private UUID actorId;

    @BeforeEach
    void prepareCanonicalSchemaDependencies() throws Exception {
        resetCanonicalFixtures();
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'TEST', 'Tenant de prueba', 'tenant-de-prueba', 'test')
                        """)
                .param("tenantId", TENANT_ID)
                .update();
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.unidad_medida (codigo, denominacion, simbolo)
                        VALUES ('UND', 'Unidad', 'und')
                        """).update();
        realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        var gestorSession = realLogin.login(TENANT_ID, "catalogo.gestor", "^catalogo\\.");
        gestor = gestorSession.bearer();
        actorId = gestorSession.userId();
        consultor = realLogin.login(TENANT_ID, "catalogo.consultor", "^catalogo\\..*\\.consultar$").bearer();
        viewer = realLogin.login(TENANT_ID, "catalogo.viewer", "^catalogo\\..*\\.consultar$").bearer();
    }

    @Test
    void managesCategoriaProductoLifecycleAndListing() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"MEDICAMENTOS","nombre":"Medicamentos",
                                 "descripcion":"Categoria raiz","nivel":1,"orden":1}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("MEDICAMENTOS"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String categoriaId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/categorias/{categoriaId}", categoriaId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"MEDICAMENTOS","nombre":"Medicamentos actualizados",
                                 "descripcion":"Categoria raiz actualizada","nivel":1,"orden":1}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Medicamentos actualizados"));

        var subCategoriaResponse = mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"categoriaPadreId":"%s","codigo":"ANALGESICOS",
                                 "nombre":"Analgesicos","nivel":2,"orden":1}
                                """.formatted(categoriaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaPadreId").value(categoriaId))
                .andReturn().getResponse().getContentAsString();
        String subCategoriaId = JsonPath.read(subCategoriaResponse, "$.id");

        mockMvc.perform(patch("/api/v1/catalogo/categorias/{categoriaId}/estado", categoriaId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVO"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/categorias")
                        .header("Authorization", consultor)
                        .param("categoriaPadreId", categoriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(subCategoriaId)).exists());

        // Sin categoriaPadreId (filtro UUID opcional omitido): confirma el fix del bind NULL sin tipo.
        mockMvc.perform(get("/api/v1/catalogo/categorias")
                        .header("Authorization", consultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(categoriaId)).exists())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(subCategoriaId)).exists());
    }

    @Test
    void managesMarcaLifecycleAndListing() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/marcas")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"GENFAR","nombre":"Genfar","descripcion":"Marca generica"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("GENFAR"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String marcaId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/marcas/{marcaId}", marcaId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"GENFAR","nombre":"Genfar actualizada",
                                 "descripcion":"Marca generica actualizada"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Genfar actualizada"));

        mockMvc.perform(patch("/api/v1/catalogo/marcas/{marcaId}/estado", marcaId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"INACTIVO"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/marcas")
                        .header("Authorization", consultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')].estado".formatted(marcaId)).value("INACTIVO"));
    }

    @Test
    void searchesAndPaginatesMarcas() throws Exception {
        crearMarca("BAYER", "Bayer");
        crearMarca("PFIZER", "Pfizer");
        crearMarca("ROCHE", "Roche");

        mockMvc.perform(get("/api/v1/catalogo/marcas")
                        .header("Authorization", consultor)
                        .param("q", "pfi"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].codigo").value("PFIZER"))
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/v1/catalogo/marcas")
                        .header("Authorization", consultor)
                        .param("page", "0")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(2))
                .andExpect(jsonPath("$.totalElements").value(3));

        mockMvc.perform(get("/api/v1/catalogo/marcas")
                        .header("Authorization", consultor)
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.totalElements").value(3));
    }

    private void crearMarca(String codigo, String nombre) throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/marcas")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"%s","nombre":"%s"}
                                """.formatted(codigo, nombre)))
                .andExpect(status().isCreated());
    }

    @Test
    void managesProductoReguladoLifecycleAndPrincipiosActivos() throws Exception {
        var principioActivoId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.principio_activo (uuid_publico, codigo_fuente, denominacion)
                        VALUES (:id, 'PARAC-500', 'Paracetamol')
                        """).param("id", principioActivoId).update();

        var createResponse = mockMvc.perform(post("/api/v1/catalogo/productos-regulados")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoProducto":"MEDICAMENTO","denominacion":"Paracetamol 500mg"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.denominacion").value("Paracetamol 500mg"))
                .andExpect(jsonPath("$.estadoRegulatorio").value("VIGENTE"))
                .andReturn().getResponse().getContentAsString();
        String productoReguladoId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(get("/api/v1/catalogo/productos-regulados/{productoReguladoId}", productoReguladoId)
                        .header("Authorization", consultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productoReguladoId));

        mockMvc.perform(put("/api/v1/catalogo/productos-regulados/{productoReguladoId}", productoReguladoId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoProducto":"MEDICAMENTO","denominacion":"Paracetamol 500mg actualizado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Paracetamol 500mg actualizado"));

        mockMvc.perform(patch("/api/v1/catalogo/productos-regulados/{productoReguladoId}/estado", productoReguladoId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUSPENDIDO"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/productos-regulados/{productoReguladoId}", productoReguladoId)
                        .header("Authorization", consultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRegulatorio").value("SUSPENDIDO"));

        mockMvc.perform(get("/api/v1/catalogo/productos-regulados")
                        .header("Authorization", consultor)
                        .param("q", "Paracetamol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(productoReguladoId)).exists());

        mockMvc.perform(post(
                        "/api/v1/catalogo/productos-regulados/{productoReguladoId}/principios-activos",
                        productoReguladoId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principioActivoId":"%s","concentracionTexto":"500 mg","esPrincipal":true,"orden":1}
                                """.formatted(principioActivoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principiosActivos[0].principioActivoId").value(principioActivoId.toString()));

        mockMvc.perform(delete(
                        "/api/v1/catalogo/productos-regulados/{productoReguladoId}/principios-activos/{principioActivoId}",
                        productoReguladoId, principioActivoId)
                        .header("Authorization", gestor).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principiosActivos").isEmpty());
    }

    @Test
    void managesSkuLifecycleAndFractionSale() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/skus")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-001",
                                 "descripcionComercial":"Producto sin receta","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoInterno").value("SKU-001"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String skuId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/skus/{skuId}", skuId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-001",
                                 "descripcionComercial":"Producto sin receta actualizado","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcionComercial").value("Producto sin receta actualizado"));

        mockMvc.perform(patch("/api/v1/catalogo/skus/{skuId}/estado", skuId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"BLOQUEADO"}
                                """))
                .andExpect(status().isNoContent());

        // Confirma que el GET refleja el nuevo estado dentro de la misma transaccion de prueba:
        // changeSkuStatus(...) limpia el EntityManager tras el UPDATE JDBC directo para que
        // findSkuById(...) (JPA) no sirva la entidad obsoleta desde el cache de primer nivel.
        mockMvc.perform(get("/api/v1/catalogo/skus/{skuId}", skuId)
                        .header("Authorization", consultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(skuId))
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"));

        var categoriaParaFiltroResponse = mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"FILTRO-SKU","nombre":"Filtro SKU","nivel":1,"orden":1}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoriaParaFiltroId = JsonPath.read(categoriaParaFiltroResponse, "$.id");

        var marcaParaFiltroResponse = mockMvc.perform(post("/api/v1/catalogo/marcas")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"FILTRO-SKU","nombre":"Filtro SKU"}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String marcaParaFiltroId = JsonPath.read(marcaParaFiltroResponse, "$.id");

        mockMvc.perform(get("/api/v1/catalogo/skus")
                        .header("Authorization", consultor)
                        .param("categoriaId", categoriaParaFiltroId)
                        .param("marcaId", marcaParaFiltroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(skuId)).doesNotExist());

        // Sin categoriaId/marcaId (filtros UUID opcionales omitidos): confirma el fix del bind NULL sin tipo.
        mockMvc.perform(get("/api/v1/catalogo/skus")
                        .header("Authorization", consultor))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(skuId)).exists());

        mockMvc.perform(post("/api/v1/catalogo/skus")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-002",
                                 "descripcionComercial":"Producto con venta por fraccion","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":true,"factorFraccion":0.5,"unidadFraccionCodigo":"UND",
                                 "requiereLote":false,"requiereVencimiento":false,"afectoIgv":true,
                                 "stockMinimoDefault":0}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permiteVentaFraccion").value(true))
                .andExpect(jsonPath("$.factorFraccion").value(0.5));
    }

    @Test
    void managesSkuBarcodesLifecycle() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/skus")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-BARRA",
                                 "descripcionComercial":"Producto con codigos de barra","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String skuId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(post("/api/v1/catalogo/skus/{skuId}/codigos-barra", skuId)
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigoBarra":"7750001234567"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigosBarra[?(@.codigoBarra=='7750001234567')]").exists());

        mockMvc.perform(patch("/api/v1/catalogo/skus/{skuId}/codigos-barra/{codigoBarra}/principal",
                        skuId, "7750001234567")
                        .header("Authorization", gestor).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigosBarra[0].codigoBarra").value("7750001234567"))
                .andExpect(jsonPath("$.codigosBarra[0].esPrincipal").value(true));

        mockMvc.perform(delete("/api/v1/catalogo/skus/{skuId}/codigos-barra/{codigoBarra}", skuId, "7750001234567")
                        .header("Authorization", gestor).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigosBarra").isEmpty());
    }

    @Test
    void storesTheFullActorOfTheTokenAndRequiresTheUnidadDeVenta() throws Exception {
        var body = """
                {"tipoSku":"NO_REGULADO","codigoInterno":"SKU-ACTOR","descripcionComercial":"Producto con actor",
                 "unidadVentaCodigo":"UND","permiteVentaFraccion":false,"requiereLote":false,
                 "requiereVencimiento":false,"afectoIgv":true,"stockMinimoDefault":0}
                """;
        var created = mockMvc.perform(post("/api/v1/catalogo/skus").header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tenantId").value(TENANT_ID.toString()))
                .andExpect(jsonPath("$.createdBy").value(actorId.toString()))
                .andReturn().getResponse().getContentAsString();
        String skuId = JsonPath.read(created, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/skus/{skuId}", skuId).header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.updatedBy").value(actorId.toString()));

        var actors = jdbcClient.sql("SELECT created_by, updated_by FROM sch_catalogo.sku_comercial "
                        + "WHERE uuid_publico = :id")
                .param("id", UUID.fromString(skuId)).query().singleRow();
        org.assertj.core.api.Assertions.assertThat(actors)
                .containsEntry("created_by", actorId.toString()).containsEntry("updated_by", actorId.toString());

        mockMvc.perform(post("/api/v1/catalogo/skus").header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body.replace("\"unidadVentaCodigo\":\"UND\",", "")
                                .replace("SKU-ACTOR", "SKU-SIN-UNIDAD")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors[0].field").value("unidadVentaCodigo"))
                .andExpect(jsonPath("$.errors[0].message").value("La unidad de venta es obligatoria."));
    }

    @Test
    void deniesCatalogoAdministrationWithoutTheRequiredPermission() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .header("Authorization", viewer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"SIN_PERMISO","nombre":"Sin permiso","nivel":1,"orden":1}
                                """))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/catalogo/skus")
                        .header("Authorization", viewer).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoSku":"NO_REGULADO","codigoInterno":"SIN-PERMISO",
                                 "descripcionComercial":"Sin permiso","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsProblemDetailsForInvalidHttpInput() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"","nombre":"Nombre valido","nivel":1,"orden":1}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/catalogo/marcas")
                        .header("Authorization", gestor).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigo":"","nombre":"Nombre valido"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
    }

    private void resetCanonicalFixtures() {
        jdbcClient.sql("DELETE FROM sch_catalogo.sku_codigo_barra").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.sku_comercial").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.producto_principio_activo").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.producto_regulado").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.principio_activo").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.categoria_producto").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.marca").update();
        jdbcClient.sql("DELETE FROM sch_catalogo.unidad_medida").update();
        jdbcClient.sql("DELETE FROM sch_admin.tenant WHERE uuid_publico = :tenantId")
                .param("tenantId", TENANT_ID)
                .update();
    }
}
