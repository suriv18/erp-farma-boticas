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
                        INSERT INTO sch_catalogo.unidad_medida (codigo, denominacion, simbolo)
                        VALUES ('UND', 'Unidad', 'und')
                        """).update();
    }

    @Test
    void managesCategoriaProductoLifecycleAndListing() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"MEDICAMENTOS","nombre":"Medicamentos",
                                 "descripcion":"Categoria raiz","nivel":1,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("MEDICAMENTOS"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String categoriaId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/categorias/{categoriaId}", categoriaId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"MEDICAMENTOS","nombre":"Medicamentos actualizados",
                                 "descripcion":"Categoria raiz actualizada","nivel":1,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Medicamentos actualizados"));

        var subCategoriaResponse = mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","categoriaPadreId":"%s","codigo":"ANALGESICOS",
                                 "nombre":"Analgesicos","nivel":2,"orden":1}
                                """.formatted(TENANT_ID, categoriaId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.categoriaPadreId").value(categoriaId))
                .andReturn().getResponse().getContentAsString();
        String subCategoriaId = JsonPath.read(subCategoriaResponse, "$.id");

        mockMvc.perform(patch("/api/v1/catalogo/categorias/{categoriaId}/estado", categoriaId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","status":"INACTIVO"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/categorias")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString())
                        .param("categoriaPadreId", categoriaId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='%s')]".formatted(subCategoriaId)).exists());

        // Sin categoriaPadreId (filtro UUID opcional omitido): confirma el fix del bind NULL sin tipo.
        mockMvc.perform(get("/api/v1/catalogo/categorias")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='%s')]".formatted(categoriaId)).exists())
                .andExpect(jsonPath("$[?(@.id=='%s')]".formatted(subCategoriaId)).exists());
    }

    @Test
    void managesMarcaLifecycleAndListing() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/marcas")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"GENFAR","nombre":"Genfar","descripcion":"Marca generica"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigo").value("GENFAR"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String marcaId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/marcas/{marcaId}", marcaId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"GENFAR","nombre":"Genfar actualizada",
                                 "descripcion":"Marca generica actualizada"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Genfar actualizada"));

        mockMvc.perform(patch("/api/v1/catalogo/marcas/{marcaId}/estado", marcaId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","status":"INACTIVO"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/marcas")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.id=='%s')].estado".formatted(marcaId)).value("INACTIVO"));
    }

    @Test
    void managesProductoReguladoLifecycleAndPrincipiosActivos() throws Exception {
        var principioActivoId = UUID.randomUUID();
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.principio_activo (uuid_publico, codigo_fuente, denominacion)
                        VALUES (:id, 'PARAC-500', 'Paracetamol')
                        """).param("id", principioActivoId).update();

        var createResponse = mockMvc.perform(post("/api/v1/catalogo/productos-regulados")
                        .with(gestor()).with(csrf())
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
                        .with(consultor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(productoReguladoId));

        mockMvc.perform(put("/api/v1/catalogo/productos-regulados/{productoReguladoId}", productoReguladoId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tipoProducto":"MEDICAMENTO","denominacion":"Paracetamol 500mg actualizado"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.denominacion").value("Paracetamol 500mg actualizado"));

        mockMvc.perform(patch("/api/v1/catalogo/productos-regulados/{productoReguladoId}/estado", productoReguladoId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"SUSPENDIDO"}
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/v1/catalogo/productos-regulados/{productoReguladoId}", productoReguladoId)
                        .with(consultor()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estadoRegulatorio").value("SUSPENDIDO"));

        mockMvc.perform(get("/api/v1/catalogo/productos-regulados")
                        .with(consultor())
                        .param("q", "Paracetamol"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(productoReguladoId)).exists());

        mockMvc.perform(post(
                        "/api/v1/catalogo/productos-regulados/{productoReguladoId}/principios-activos",
                        productoReguladoId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"principioActivoId":"%s","concentracionTexto":"500 mg","esPrincipal":true,"orden":1}
                                """.formatted(principioActivoId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principiosActivos[0].principioActivoId").value(principioActivoId.toString()));

        mockMvc.perform(delete(
                        "/api/v1/catalogo/productos-regulados/{productoReguladoId}/principios-activos/{principioActivoId}",
                        productoReguladoId, principioActivoId)
                        .with(gestor()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.principiosActivos").isEmpty());
    }

    @Test
    void managesSkuLifecycleAndFractionSale() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/skus")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","tipoSku":"NO_REGULADO","codigoInterno":"SKU-001",
                                 "descripcionComercial":"Producto sin receta","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.codigoInterno").value("SKU-001"))
                .andExpect(jsonPath("$.estado").value("ACTIVO"))
                .andReturn().getResponse().getContentAsString();
        String skuId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(put("/api/v1/catalogo/skus/{skuId}", skuId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","tipoSku":"NO_REGULADO","codigoInterno":"SKU-001",
                                 "descripcionComercial":"Producto sin receta actualizado","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.descripcionComercial").value("Producto sin receta actualizado"));

        mockMvc.perform(patch("/api/v1/catalogo/skus/{skuId}/estado", skuId)
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","status":"BLOQUEADO"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isNoContent());

        // Confirma que el GET refleja el nuevo estado dentro de la misma transaccion de prueba:
        // changeSkuStatus(...) limpia el EntityManager tras el UPDATE JDBC directo para que
        // findSkuById(...) (JPA) no sirva la entidad obsoleta desde el cache de primer nivel.
        mockMvc.perform(get("/api/v1/catalogo/skus/{skuId}", skuId)
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(skuId))
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"));

        var categoriaParaFiltroResponse = mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"FILTRO-SKU","nombre":"Filtro SKU","nivel":1,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String categoriaParaFiltroId = JsonPath.read(categoriaParaFiltroResponse, "$.id");

        var marcaParaFiltroResponse = mockMvc.perform(post("/api/v1/catalogo/marcas")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"FILTRO-SKU","nombre":"Filtro SKU"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String marcaParaFiltroId = JsonPath.read(marcaParaFiltroResponse, "$.id");

        mockMvc.perform(get("/api/v1/catalogo/skus")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString())
                        .param("categoriaId", categoriaParaFiltroId)
                        .param("marcaId", marcaParaFiltroId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(skuId)).doesNotExist());

        // Sin categoriaId/marcaId (filtros UUID opcionales omitidos): confirma el fix del bind NULL sin tipo.
        mockMvc.perform(get("/api/v1/catalogo/skus")
                        .with(consultor())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[?(@.id=='%s')]".formatted(skuId)).exists());

        mockMvc.perform(post("/api/v1/catalogo/skus")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","tipoSku":"NO_REGULADO","codigoInterno":"SKU-002",
                                 "descripcionComercial":"Producto con venta por fraccion","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":true,"factorFraccion":0.5,"unidadFraccionCodigo":"UND",
                                 "requiereLote":false,"requiereVencimiento":false,"afectoIgv":true,
                                 "stockMinimoDefault":0}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.permiteVentaFraccion").value(true))
                .andExpect(jsonPath("$.factorFraccion").value(0.5));
    }

    @Test
    void managesSkuBarcodesLifecycle() throws Exception {
        var createResponse = mockMvc.perform(post("/api/v1/catalogo/skus")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","tipoSku":"NO_REGULADO","codigoInterno":"SKU-BARRA",
                                 "descripcionComercial":"Producto con codigos de barra","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        String skuId = JsonPath.read(createResponse, "$.id");

        mockMvc.perform(post("/api/v1/catalogo/skus/{skuId}/codigos-barra", skuId)
                        .with(gestor()).with(csrf())
                        .param("tenantId", TENANT_ID.toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"codigoBarra":"7750001234567"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigosBarra[?(@.codigoBarra=='7750001234567')]").exists());

        mockMvc.perform(patch("/api/v1/catalogo/skus/{skuId}/codigos-barra/{codigoBarra}/principal",
                        skuId, "7750001234567")
                        .with(gestor()).with(csrf())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigosBarra[0].codigoBarra").value("7750001234567"))
                .andExpect(jsonPath("$.codigosBarra[0].esPrincipal").value(true));

        mockMvc.perform(delete("/api/v1/catalogo/skus/{skuId}/codigos-barra/{codigoBarra}", skuId, "7750001234567")
                        .with(gestor()).with(csrf())
                        .param("tenantId", TENANT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.codigosBarra").isEmpty());
    }

    @Test
    void deniesCatalogoAdministrationWithoutTheRequiredPermission() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"SIN_PERMISO","nombre":"Sin permiso","nivel":1,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isForbidden());

        mockMvc.perform(post("/api/v1/catalogo/skus")
                        .with(SecurityMockMvcRequestPostProcessors.user("viewer")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","tipoSku":"NO_REGULADO","codigoInterno":"SIN-PERMISO",
                                 "descripcionComercial":"Sin permiso","unidadVentaCodigo":"UND",
                                 "permiteVentaFraccion":false,"requiereLote":false,"requiereVencimiento":false,
                                 "afectoIgv":true,"stockMinimoDefault":0}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isForbidden());
    }

    @Test
    void returnsProblemDetailsForInvalidHttpInput() throws Exception {
        mockMvc.perform(post("/api/v1/catalogo/categorias")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"","nombre":"Nombre valido","nivel":1,"orden":1}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));

        mockMvc.perform(post("/api/v1/catalogo/marcas")
                        .with(gestor()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"tenantId":"%s","codigo":"","nombre":"Nombre valido"}
                                """.formatted(TENANT_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("REQUEST_VALIDATION_FAILED"));
    }

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor gestor() {
        return SecurityMockMvcRequestPostProcessors.user("admin").authorities(
                new SimpleGrantedAuthority("catalogo.categorias.gestionar"),
                new SimpleGrantedAuthority("catalogo.categorias.consultar"),
                new SimpleGrantedAuthority("catalogo.marcas.gestionar"),
                new SimpleGrantedAuthority("catalogo.marcas.consultar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.gestionar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.consultar"),
                new SimpleGrantedAuthority("catalogo.skus.gestionar"),
                new SimpleGrantedAuthority("catalogo.skus.consultar"));
    }

    private static SecurityMockMvcRequestPostProcessors.UserRequestPostProcessor consultor() {
        return SecurityMockMvcRequestPostProcessors.user("consultor").authorities(
                new SimpleGrantedAuthority("catalogo.categorias.consultar"),
                new SimpleGrantedAuthority("catalogo.marcas.consultar"),
                new SimpleGrantedAuthority("catalogo.productos-regulados.consultar"),
                new SimpleGrantedAuthority("catalogo.skus.consultar"));
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
        jdbcClient.sql("DELETE FROM sch_admin.tenant").update();
    }
}
