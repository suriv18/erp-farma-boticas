package com.softprimesolutions.inventario.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.softprimesolutions.security.application.port.out.PasswordHashPort;
import com.softprimesolutions.testsupport.PostgresTestContainerConfiguration;
import com.softprimesolutions.testsupport.RealLogin;
import java.time.LocalDate;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@ActiveProfiles("test")
@Import(PostgresTestContainerConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class InventarioApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("5a1c3e7d-2b4f-4d6a-8c90-1e2f3a4b5c6d");
    private static final String ORG = "/api/v1/organizacion";
    private static final String INV = "/api/v1/inventario";
    private static final String UNIDAD = "UNDINV";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private RealLogin realLogin;
    private String noPermissions;

    private UUID userId;
    private String accessToken;
    private UUID almacenId;
    private UUID skuId;

    @BeforeEach
    void createTenantAdministratorAndTheMasterDataOfAWarehouseAndASku() throws Exception {
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'INVTEST', 'Tenant inventario', 'tenant-inventario', 'test')
                        """).param("tenantId", TENANT_ID).update();
        realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        var session = realLogin.login(TENANT_ID, "inv.admin", "^(organizacion|inventario)\\.");
        userId = session.userId();
        accessToken = session.bearer();
        noPermissions = realLogin.login(TENANT_ID, "sin.permisos", null).bearer();

        var empresaId = created(post(ORG + "/empresas").content("""
                {"tenantId":"%s","ruc":"20123456786","razonSocial":"Boticas Inventario SAC",
                 "monedaFuncional":"PEN","zonaHoraria":"America/Lima","permiteVentaOnline":false}
                """.formatted(TENANT_ID)));
        var establecimientoId = created(post(ORG + "/establecimientos").content("""
                {"tenantId":"%s","empresaId":"%s","codigo":"EST001","nombre":"Botica Central",
                 "tipoEstablecimiento":"BOTICA","codigoAnexoSunat":"0001","esPrincipal":true,
                 "permiteVentaOnline":false,"permiteDelivery":false,"perfilOperacion":"ONLINE",
                 "zonaHoraria":"America/Lima"}
                """.formatted(TENANT_ID, empresaId)));
        almacenId = created(post(ORG + "/almacenes").content("""
                {"tenantId":"%s","establecimientoId":"%s","codigo":"ALM001","nombre":"Almacen Central",
                 "tipo":"GENERAL","permiteLotes":true,"permiteVencimiento":true,"permiteVenta":true,
                 "permiteDespacho":true,"controlTemperatura":false}
                """.formatted(TENANT_ID, establecimientoId)));

        skuId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO sch_catalogo.unidad_medida (codigo, denominacion) VALUES (:codigo, 'Unidad')")
                .param("codigo", UNIDAD).update();
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.sku_comercial
                            (uuid_publico, tenant_id, tipo_sku, codigo_interno, descripcion_comercial,
                             unidad_venta_codigo)
                        SELECT :skuId, t.id, 'NO_REGULADO', 'SKU-INV-1', 'Producto de prueba', :unidad
                          FROM sch_admin.tenant t WHERE t.uuid_publico = :tenantId
                        """).param("skuId", skuId).param("unidad", UNIDAD).param("tenantId", TENANT_ID).update();
    }

    @Test
    void registersIngresoAndSalidaUpdatingStockAndKardexAsTheDdlDefines() throws Exception {
        var vencimiento = LocalDate.now().plusYears(1);

        var ingreso = movimiento("AJUSTE_INGRESO", "10", "Saldo inicial", null, "L-001", vencimiento)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.tipo").value("AJUSTE_INGRESO"))
                .andExpect(jsonPath("$.naturaleza").value("E"))
                .andExpect(jsonPath("$.stockAnterior").value(0))
                .andExpect(jsonPath("$.stockPosterior").value(10))
                .andReturn().getResponse().getContentAsString();
        var loteId = UUID.fromString(JsonPath.read(ingreso, "$.loteId"));

        movimiento("AJUSTE_SALIDA", "4", "Merma por rotura", loteId, null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.naturaleza").value("S"))
                .andExpect(jsonPath("$.stockAnterior").value(10))
                .andExpect(jsonPath("$.stockPosterior").value(6));

        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer())
                        .param("almacenId", almacenId.toString()).param("skuId", skuId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].loteId").value(loteId.toString()))
                .andExpect(jsonPath("$.items[0].numeroLote").value("L-001"))
                .andExpect(jsonPath("$.items[0].estadoInventario").value("DISPONIBLE"))
                .andExpect(jsonPath("$.items[0].estadoLote").value("HABILITADO"))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(6))
                .andExpect(jsonPath("$.items[0].cantidadReservada").value(0))
                .andExpect(jsonPath("$.items[0].cantidadDisponible").value(6))
                .andExpect(jsonPath("$.items[0].vendible").value(true))
                .andExpect(jsonPath("$.items[0].version").value(1))
                .andExpect(jsonPath("$.items[0].almacenId").value(almacenId.toString()))
                .andExpect(jsonPath("$.items[0].skuId").value(skuId.toString()));

        var kardex = jdbcClient.sql("""
                        SELECT m.tipo_movimiento, m.tipo_operacion_sunat, m.naturaleza, m.cantidad, m.stock_anterior,
                               m.stock_posterior, m.documento_tipo, m.actor, m.observacion,
                               m.metadata ->> 'huellaSolicitud' AS huella,
                               m.almacen_origen_id IS NULL AS sin_origen, m.almacen_destino_id IS NULL AS sin_destino
                          FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId ORDER BY m.id
                        """).param("tenantId", TENANT_ID).query().listOfRows();
        assertThat(kardex).hasSize(2);
        assertThat(kardex.get(0)).containsEntry("tipo_movimiento", "AJUSTE_INGRESO")
                .containsEntry("tipo_operacion_sunat", "99").containsEntry("naturaleza", "E")
                .containsEntry("documento_tipo", "AJUSTE_MANUAL").containsEntry("observacion", "Saldo inicial")
                .containsEntry("actor", userId.toString()).containsEntry("sin_origen", true)
                .containsEntry("sin_destino", false);
        assertThat(kardex.get(0).get("huella")).isNotNull();
        assertThat(kardex.get(1)).containsEntry("tipo_movimiento", "AJUSTE_SALIDA")
                .containsEntry("naturaleza", "S").containsEntry("sin_origen", false)
                .containsEntry("sin_destino", true);
        assertThat((java.math.BigDecimal) kardex.get(1).get("stock_anterior")).isEqualByComparingTo("10");
        assertThat((java.math.BigDecimal) kardex.get(1).get("stock_posterior")).isEqualByComparingTo("6");

        var posicion = jdbcClient.sql("""
                        SELECT p.estado_inventario, p.cantidad_fisica, p.cantidad_reservada, p.cantidad_disponible,
                               p.version_lock, p.ubicacion_id IS NULL AS sin_ubicacion,
                               p.ultimo_movimiento_at IS NOT NULL AS con_movimiento
                          FROM sch_inventario.posicion_inventario p
                          JOIN sch_admin.tenant t ON t.id = p.tenant_id WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).query().singleRow();
        assertThat(posicion).containsEntry("estado_inventario", "DISPONIBLE").containsEntry("version_lock", 1L)
                .containsEntry("sin_ubicacion", true).containsEntry("con_movimiento", true);
        assertThat((java.math.BigDecimal) posicion.get("cantidad_fisica")).isEqualByComparingTo("6");
        assertThat((java.math.BigDecimal) posicion.get("cantidad_disponible")).isEqualByComparingTo("6");

        var lote = jdbcClient.sql("""
                        SELECT l.numero_lote, l.estado_lote, l.es_activo, l.created_by
                          FROM sch_inventario.lote l JOIN sch_admin.tenant t ON t.id = l.tenant_id
                         WHERE t.uuid_publico = :tenantId
                        """).param("tenantId", TENANT_ID).query().singleRow();
        assertThat(lote).containsEntry("numero_lote", "L-001").containsEntry("estado_lote", "HABILITADO")
                .containsEntry("es_activo", "1");
    }

    @Test
    void rejectsASalidaThatExceedsTheStockWithoutChangingIt() throws Exception {
        var loteId = ingresar("5", "L-002");

        movimiento("AJUSTE_SALIDA", "6", "Merma", loteId, null, null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INV_STOCK_INSUFICIENTE"));

        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer()))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(5))
                .andExpect(jsonPath("$.items[0].version").value(0));
    }

    @Test
    void ingresoIntoAnExistingLoteAccumulatesOnTheSamePosition() throws Exception {
        var loteId = ingresar("5", "L-003");

        movimiento("AJUSTE_INGRESO", "2.5", "Reposicion", loteId, null, null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockPosterior").value(7.5));

        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer()))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(7.5));
    }

    @Test
    void blockingALoteStopsItBeingSellableAndUnblockingRestoresIt() throws Exception {
        var loteId = ingresar("5", "L-004");

        mockMvc.perform(post(INV + "/lotes/{id}/bloqueos", loteId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Reclamo de calidad\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("BLOQUEADO"))
                .andExpect(jsonPath("$.motivoEstado").value("Reclamo de calidad"))
                .andExpect(jsonPath("$.vendible").value(false));
        var bloqueado = jdbcClient.sql("""
                        SELECT l.estado_lote, l.motivo_estado, l.bloqueado_por, l.bloqueado_at IS NOT NULL AS con_fecha
                          FROM sch_inventario.lote l WHERE l.uuid_publico = :loteId
                        """).param("loteId", loteId).query().singleRow();
        assertThat(bloqueado).containsEntry("estado_lote", "BLOQUEADO")
                .containsEntry("motivo_estado", "Reclamo de calidad").containsEntry("con_fecha", true);
        assertThat(bloqueado.get("bloqueado_por")).isEqualTo(userId.toString());
        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer()))
                .andExpect(jsonPath("$.items[0].vendible").value(false))
                .andExpect(jsonPath("$.items[0].estadoLote").value("BLOQUEADO"));
        mockMvc.perform(post(INV + "/lotes/{id}/bloqueos", loteId).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"otra vez\"}"))
                .andExpect(status().isConflict());

        mockMvc.perform(delete(INV + "/lotes/{id}/bloqueos", loteId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("HABILITADO"))
                .andExpect(jsonPath("$.vendible").value(true));
        var habilitado = jdbcClient.sql("""
                        SELECT l.estado_lote, l.motivo_estado, l.bloqueado_por, l.bloqueado_at
                          FROM sch_inventario.lote l WHERE l.uuid_publico = :loteId
                        """).param("loteId", loteId).query().singleRow();
        assertThat(habilitado.get("estado_lote")).isEqualTo("HABILITADO");
        assertThat(habilitado.get("motivo_estado")).isNull();
        assertThat(habilitado.get("bloqueado_por")).isNull();
        assertThat(habilitado.get("bloqueado_at")).isNull();
        mockMvc.perform(delete(INV + "/lotes/{id}/bloqueos", loteId).header("Authorization", bearer()))
                .andExpect(status().isConflict());
        mockMvc.perform(get(INV + "/lotes/{id}", loteId).header("Authorization", bearer()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroLote").value("L-004"))
                .andExpect(jsonPath("$.skuId").value(skuId.toString()))
                .andExpect(jsonPath("$.vendible").value(true));
    }

    @Test
    void validatesTheMovementAndItsReferences() throws Exception {
        var vencimiento = LocalDate.now().plusYears(1);

        movimiento("AJUSTE_INGRESO", "5", "x", null, "L-005", LocalDate.now().minusDays(1))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INV_LOTE_VENCIDO"));
        movimiento("TRASLADO", "5", "x", null, "L-005", vencimiento).andExpect(status().isBadRequest());
        movimiento("AJUSTE_INGRESO", "0", "x", null, "L-005", vencimiento).andExpect(status().isBadRequest());
        movimiento("AJUSTE_INGRESO", "5", "", null, "L-005", vencimiento).andExpect(status().isBadRequest());
        movimiento("AJUSTE_SALIDA", "5", "x", null, "L-005", vencimiento)
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INV_LOTE_REFERENCIA_INVALIDA"));
        movimiento("AJUSTE_SALIDA", "5", "x", UUID.randomUUID(), null, null)
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INV_LOTE_NO_ENCONTRADO"));
        mockMvc.perform(post(INV + "/movimientos").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(
                                UUID.randomUUID(), skuId, "AJUSTE_INGRESO", "5", "x", null, "L-005", vencimiento)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INV_ALMACEN_NO_ENCONTRADO"));
        mockMvc.perform(post(INV + "/movimientos").header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content(json(
                                almacenId, UUID.randomUUID(), "AJUSTE_INGRESO", "5", "x", null, "L-005", vencimiento)))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("INV_SKU_NO_ENCONTRADO"));
    }

    @Test
    void rejectsMovementsOnAnInactiveWarehouseOrInactiveSku() throws Exception {
        var vencimiento = LocalDate.now().plusYears(1);

        jdbcClient.sql("UPDATE sch_catalogo.sku_comercial SET estado_comercial = 'BLOQUEADO' WHERE uuid_publico = :id")
                .param("id", skuId).update();
        movimiento("AJUSTE_INGRESO", "5", "x", null, "L-006", vencimiento)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INV_SKU_NO_OPERABLE"));
        jdbcClient.sql("UPDATE sch_catalogo.sku_comercial SET estado_comercial = 'ACTIVO' WHERE uuid_publico = :id")
                .param("id", skuId).update();
        jdbcClient.sql("UPDATE sch_organizacion.almacen SET es_activo = '0' WHERE uuid_publico = :id")
                .param("id", almacenId).update();
        movimiento("AJUSTE_INGRESO", "5", "x", null, "L-006", vencimiento)
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("INV_ALMACEN_NO_OPERABLE"));
    }

    @Test
    void validatesTheBlockRequestAndUnknownLotes() throws Exception {
        mockMvc.perform(post(INV + "/lotes/{id}/bloqueos", UUID.randomUUID()).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"\"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(INV + "/lotes/{id}/bloqueos", UUID.randomUUID()).header("Authorization", bearer())
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"x\"}"))
                .andExpect(status().isNotFound());
        mockMvc.perform(delete(INV + "/lotes/{id}/bloqueos", UUID.randomUUID()).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(INV + "/lotes/{id}", UUID.randomUUID()).header("Authorization", bearer()))
                .andExpect(status().isNotFound());
        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer()).param("size", "500"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deniesAccessWithoutTheRequiredPermissionOrToken() throws Exception {
        mockMvc.perform(post(INV + "/movimientos").with(csrf())
                        .header("Authorization", noPermissions)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(almacenId, skuId, "AJUSTE_INGRESO", "1", "x", null, "L", LocalDate.now().plusDays(5))))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(INV + "/posiciones").header("Authorization", noPermissions))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(INV + "/posiciones")).andExpect(status().isUnauthorized());
    }

    @Test
    void aRetryWithTheSameIdempotencyKeyReturnsTheOriginalResultWithoutMovingStockTwice() throws Exception {
        var loteId = ingresar("10", "L-007");

        var primero = salidaConClave("clave-api-1", "4", loteId).andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockPosterior").value(6)).andReturn().getResponse().getContentAsString();
        var repetido = salidaConClave("clave-api-1", "4.0", loteId).andExpect(status().isCreated())
                .andExpect(jsonPath("$.stockPosterior").value(6)).andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(repetido, "$.id")).isEqualTo(JsonPath.read(primero, "$.id"));
        assertThat((String) JsonPath.read(repetido, "$.posicionId")).isEqualTo(JsonPath.read(primero, "$.posicionId"));
        assertThat(jdbcClient.sql("""
                        SELECT COUNT(*) FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.naturaleza = 'S'
                        """).param("tenantId", TENANT_ID).query(Long.class).single()).isEqualTo(1L);
        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer()))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(6))
                .andExpect(jsonPath("$.items[0].version").value(1));

        salidaConClave("clave-api-1", "5", loteId).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INV_IDEMPOTENCY_CONFLICT"));
        salidaConClave("   ", "1", loteId).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INV_IDEMPOTENCY_KEY_INVALID"));
    }

    private ResultActions salidaConClave(String clave, String cantidad, UUID loteId) throws Exception {
        return mockMvc.perform(post(INV + "/movimientos").header("Authorization", bearer())
                .header("Idempotency-Key", clave).contentType(MediaType.APPLICATION_JSON)
                .content(json(almacenId, skuId, "AJUSTE_SALIDA", cantidad, "Merma", loteId, null, null)));
    }

    private UUID ingresar(String cantidad, String numeroLote) throws Exception {
        var body = movimiento("AJUSTE_INGRESO", cantidad, "Saldo inicial", null, numeroLote,
                LocalDate.now().plusYears(1))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.loteId"));
    }

    private ResultActions movimiento(
            String tipo, String cantidad, String motivo, UUID loteId, String numeroLote, LocalDate vencimiento)
            throws Exception {
        return mockMvc.perform(post(INV + "/movimientos").header("Authorization", bearer())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(almacenId, skuId, tipo, cantidad, motivo, loteId, numeroLote, vencimiento)));
    }

    private static String json(
            UUID almacen, UUID sku, String tipo, String cantidad, String motivo, UUID loteId, String numeroLote,
            LocalDate vencimiento) {
        return """
                {"almacenId":"%s","skuId":"%s","tipo":"%s","cantidad":%s,"motivo":"%s",
                 "loteId":%s,"numeroLote":%s,"fechaVencimiento":%s}
                """.formatted(almacen, sku, tipo, cantidad, motivo,
                loteId == null ? "null" : "\"" + loteId + "\"",
                numeroLote == null ? "null" : "\"" + numeroLote + "\"",
                vencimiento == null ? "null" : "\"" + vencimiento + "\"");
    }

    private UUID created(MockHttpServletRequestBuilder request) throws Exception {
        var body = mockMvc.perform(request.header("Authorization", bearer()).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }

    private String bearer() {
        return accessToken;
    }
}
