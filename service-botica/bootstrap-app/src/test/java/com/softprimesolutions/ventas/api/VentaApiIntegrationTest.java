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
import java.math.BigDecimal;
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
class VentaApiIntegrationTest {

    private static final UUID TENANT_ID = UUID.fromString("c8f2e3d4-5b6a-4f7e-8d9c-2b3c4d5e6f70");
    private static final String ORG = "/api/v1/organizacion";
    private static final String INV = "/api/v1/inventario";
    private static final String VENTAS = "/api/v1/ventas/ventas";
    private static final String TURNOS = "/api/v1/ventas/turnos";
    private static final String ANULAR = VENTAS + "/{id}/anulacion";
    private static final String UNIDAD = "UNDVEN";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcClient jdbcClient;

    @Autowired
    private PasswordHashPort passwordHash;

    private String bearer;
    private String noPermissions;
    private UUID userId;
    private UUID empresaId;
    private UUID establecimientoId;
    private UUID almacenId;
    private UUID terminalId;
    private UUID skuId;

    @BeforeEach
    void createTheTenantTheMasterDataAndAnOpenTurno() throws Exception {
        jdbcClient.sql("""
                        INSERT INTO sch_admin.tenant (uuid_publico, codigo, nombre, slug, created_by)
                        VALUES (:tenantId, 'VENTVTA', 'Tenant ventas', 'tenant-ventas-venta', 'test')
                        """).param("tenantId", TENANT_ID).update();
        var realLogin = new RealLogin(jdbcClient, mockMvc, passwordHash);
        var session = realLogin.login(TENANT_ID, "venta.admin", "^(organizacion|inventario|ventas)\\.");
        userId = session.userId();
        bearer = session.bearer();
        noPermissions = realLogin.login(TENANT_ID, "sin.permisos", null).bearer();

        empresaId = created(post(ORG + "/empresas").content("""
                {"tenantId":"%s","ruc":"20123456786","razonSocial":"Boticas Venta SAC",
                 "monedaFuncional":"PEN","zonaHoraria":"America/Lima","permiteVentaOnline":false}
                """.formatted(TENANT_ID)));
        establecimientoId = created(post(ORG + "/establecimientos").content("""
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
        terminalId = created(post(ORG + "/terminales-pos").content("""
                {"establecimientoId":"%s","codigo":"POS001","nombre":"Caja 1",
                 "serieBoletaDefecto":"B001","serieFacturaDefecto":"F001","storeEdgeHabilitado":false}
                """.formatted(establecimientoId)));

        skuId = UUID.randomUUID();
        jdbcClient.sql("INSERT INTO sch_catalogo.unidad_medida (codigo, denominacion) VALUES (:codigo, 'Unidad')")
                .param("codigo", UNIDAD).update();
        jdbcClient.sql("""
                        INSERT INTO sch_catalogo.sku_comercial
                            (uuid_publico, tenant_id, tipo_sku, codigo_interno, descripcion_comercial,
                             unidad_venta_codigo)
                        SELECT :skuId, t.id, 'NO_REGULADO', 'SKU-VEN-1', 'Paracetamol 500 mg', :unidad
                          FROM sch_admin.tenant t WHERE t.uuid_publico = :tenantId
                        """).param("skuId", skuId).param("unidad", UNIDAD).param("tenantId", TENANT_ID).update();

        abrirTurno(terminalId);
    }

    @Test
    void registersASaleConsumingTheEarliestExpiryLotesFirstAndRecordingEverything() throws Exception {
        var antiguo = ingresar("3", "L-ANT", LocalDate.now().plusMonths(6));
        var nuevo = ingresar("10", "L-NEW", LocalDate.now().plusYears(1));

        var cuerpo = vender("clave-1", "5", "2.50", "20")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOperacion").value("EST001-POS001-000001"))
                .andExpect(jsonPath("$.estado").value("CONFIRMADA"))
                .andExpect(jsonPath("$.vendedorId").value(userId.toString()))
                .andExpect(jsonPath("$.subtotal").value(12.5))
                .andExpect(jsonPath("$.impuestoTotal").value(0.0))
                .andExpect(jsonPath("$.total").value(12.5))
                .andExpect(jsonPath("$.lineas[0].numeroLinea").value(1))
                .andExpect(jsonPath("$.lineas[0].descripcion").value("Paracetamol 500 mg"))
                .andExpect(jsonPath("$.lineas[0].totalLinea").value(12.5))
                .andExpect(jsonPath("$.lineas[0].lotes[0].loteId").value(antiguo.toString()))
                .andExpect(jsonPath("$.lineas[0].lotes[0].cantidad").value(3.0))
                .andExpect(jsonPath("$.lineas[0].lotes[1].loteId").value(nuevo.toString()))
                .andExpect(jsonPath("$.lineas[0].lotes[1].cantidad").value(2.0))
                .andExpect(jsonPath("$.pago.medioPago").value("EFECTIVO"))
                .andExpect(jsonPath("$.pago.monto").value(12.5))
                .andExpect(jsonPath("$.pago.montoRecibido").value(20.0))
                .andExpect(jsonPath("$.pago.vuelto").value(7.5))
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer)
                        .param("almacenId", almacenId.toString()).param("skuId", skuId.toString()))
                .andExpect(jsonPath("$.items[?(@.numeroLote == 'L-ANT')].cantidadFisica").value(0.0))
                .andExpect(jsonPath("$.items[?(@.numeroLote == 'L-NEW')].cantidadFisica").value(8.0));

        var kardex = jdbcClient.sql("""
                        SELECT m.tipo_movimiento, m.tipo_operacion_sunat, m.documento_tipo, m.documento_uuid,
                               m.actor, m.cantidad
                          FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.tipo_movimiento = 'SALIDA_VENTA'
                         ORDER BY m.id
                        """).param("tenantId", TENANT_ID).query().listOfRows();
        assertThat(kardex).hasSize(2);
        assertThat(kardex).allSatisfy(fila -> {
            assertThat(fila).containsEntry("tipo_operacion_sunat", "01").containsEntry("documento_tipo", "VENTA")
                    .containsEntry("actor", userId.toString());
            assertThat(fila.get("documento_uuid")).hasToString(ventaId.toString());
        });
        var fila = jdbcClient.sql("""
                        SELECT v.created_by, v.numero_operacion, v.idempotency_key,
                               (SELECT COUNT(*) FROM sch_venta.venta_linea_lote vll
                                  JOIN sch_venta.venta_linea vl ON vl.id = vll.venta_linea_id
                                 WHERE vl.venta_id = v.id) AS lotes,
                               (SELECT p.vuelto FROM sch_venta.pago_venta p WHERE p.venta_id = v.id) AS vuelto
                          FROM sch_venta.venta v WHERE v.uuid_publico = :ventaId
                        """).param("ventaId", ventaId).query().singleRow();
        assertThat(fila).containsEntry("created_by", userId.toString()).containsEntry("lotes", 2L)
                .containsEntry("idempotency_key", "clave-1");
        assertThat((BigDecimal) fila.get("vuelto")).isEqualByComparingTo("7.50");
    }

    @Test
    void numbersTheSalesSequentiallyPerTerminal() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));

        vender("clave-a", "1", "5", "10").andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOperacion").value("EST001-POS001-000001"));
        vender("clave-b", "1", "5", "10").andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOperacion").value("EST001-POS001-000002"));
    }

    @Test
    void terminalsWithTheSameCodeInTwoEstablishmentsOfTheSameCompanySellWithDistinctNumbers() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));
        var otroEstablecimiento = created(post(ORG + "/establecimientos").content("""
                {"tenantId":"%s","empresaId":"%s","codigo":"EST002","nombre":"Botica Norte",
                 "tipoEstablecimiento":"BOTICA","codigoAnexoSunat":"0002","esPrincipal":false,
                 "permiteVentaOnline":false,"permiteDelivery":false,"perfilOperacion":"ONLINE",
                 "zonaHoraria":"America/Lima"}
                """.formatted(TENANT_ID, empresaId)));
        var otroAlmacen = created(post(ORG + "/almacenes").content("""
                {"tenantId":"%s","establecimientoId":"%s","codigo":"ALM001","nombre":"Almacen Norte",
                 "tipo":"GENERAL","permiteLotes":true,"permiteVencimiento":true,"permiteVenta":true,
                 "permiteDespacho":true,"controlTemperatura":false}
                """.formatted(TENANT_ID, otroEstablecimiento)));
        var otraTerminal = created(post(ORG + "/terminales-pos").content("""
                {"establecimientoId":"%s","codigo":"POS001","nombre":"Caja 1",
                 "serieBoletaDefecto":"B002","serieFacturaDefecto":"F002","storeEdgeHabilitado":false}
                """.formatted(otroEstablecimiento)));
        abrirTurno(otraTerminal);
        ingresar(otroAlmacen, "10", "L-002", LocalDate.now().plusYears(1));

        vender(terminalId, almacenId, "clave-local-1").andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOperacion").value("EST001-POS001-000001"));
        vender(otraTerminal, otroAlmacen, "clave-local-2").andExpect(status().isCreated())
                .andExpect(jsonPath("$.numeroOperacion").value("EST002-POS001-000001"))
                .andExpect(jsonPath("$.establecimientoId").value(otroEstablecimiento.toString()));
    }

    @Test
    void aRetryWithTheSameKeyReturnsTheSameSaleWithoutDiscountingTwiceAndADifferentBodyConflicts() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));

        var primera = vender("clave-1", "4", "5", "50").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var segunda = vender("clave-1", "4", "5", "50").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();

        assertThat((String) JsonPath.read(segunda, "$.id")).isEqualTo(JsonPath.read(primera, "$.id"));
        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer))
                .andExpect(jsonPath("$.items[0].cantidadFisica").value(6));
        vender("clave-1", "5", "5", "50").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_IDEMPOTENCY_CONFLICT"));
    }

    @Test
    void getsAndListsTheSales() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));
        var cuerpo = vender("clave-1", "2", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(get(VENTAS + "/{id}", ventaId).header("Authorization", bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ventaId.toString()))
                .andExpect(jsonPath("$.lineas[0].lotes[0].cantidad").value(2.0));
        mockMvc.perform(get(VENTAS).header("Authorization", bearer)
                        .param("establecimientoId", establecimientoId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.items[0].id").value(ventaId.toString()))
                .andExpect(jsonPath("$.items[0].total").value(10.0));
        mockMvc.perform(get(VENTAS).header("Authorization", bearer)
                        .param("establecimientoId", UUID.randomUUID().toString()))
                .andExpect(jsonPath("$.totalElements").value(0));
        mockMvc.perform(get(VENTAS).header("Authorization", bearer).param("size", "500"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEN_PAGINACION_INVALIDA"));
        mockMvc.perform(get(VENTAS + "/{id}", UUID.randomUUID()).header("Authorization", bearer))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_VENTA_NO_ENCONTRADA"));
    }

    @Test
    void closingTheTurnoCountsTheCashSalesInTheSystemTotal() throws Exception {
        ingresar("20", "L-001", LocalDate.now().plusYears(1));
        vender("clave-1", "2", "5", "10").andExpect(status().isCreated());
        vender("clave-2", "3", "5", "20").andExpect(status().isCreated());
        var turnoId = UUID.fromString(JsonPath.read(mockMvc.perform(get(TURNOS + "/actual")
                        .header("Authorization", bearer).param("terminalId", terminalId.toString()))
                .andReturn().getResponse().getContentAsString(), "$.id"));

        mockMvc.perform(post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":124}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentasSistema").value(25.0))
                .andExpect(jsonPath("$.totalSistema").value(125.0))
                .andExpect(jsonPath("$.diferencia").value(-1.0));
        vender("clave-3", "1", "5", "10").andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_NO_ABIERTO"));
    }

    @Test
    void anOpenTurnoReportsTheConfirmedCashSalesMadeSoFar() throws Exception {
        ingresar("20", "L-001", LocalDate.now().plusYears(1));
        var anulada = vender("clave-1", "2", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        vender("clave-2", "3", "5", "20").andExpect(status().isCreated());
        mockMvc.perform(post(ANULAR, UUID.fromString(JsonPath.read(anulada, "$.id")))
                        .header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Cliente se arrepintio\"}"))
                .andExpect(status().isOk());

        var actual = mockMvc.perform(get(TURNOS + "/actual").header("Authorization", bearer)
                        .param("terminalId", terminalId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("ABIERTO"))
                .andExpect(jsonPath("$.totalVentasSistema").value(15.0))
                .andExpect(jsonPath("$.totalSistema").value(115.0))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(get(TURNOS + "/{id}", UUID.fromString(JsonPath.read(actual, "$.id")))
                        .header("Authorization", bearer))
                .andExpect(jsonPath("$.totalSistema").value(115.0));
    }

    @Test
    void validatesTheRequestAndItsReferences() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));

        vender(null, "1", "5", "10").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEN_IDEMPOTENCY_KEY_INVALID"));
        vender("clave-1", "1", "-1", "10").andExpect(status().isBadRequest());
        vender("clave-1", "0", "5", "10").andExpect(status().isBadRequest());
        vender("clave-1", "1", "5", "4").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEN_MONTO_RECIBIDO_INSUFICIENTE"));
        mockMvc.perform(post(VENTAS).header("Authorization", bearer).header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":\"%s\",\"almacenId\":\"%s\",\"lineas\":[],\"pago\":{\"montoRecibido\":1}}"
                                .formatted(terminalId, almacenId)))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(VENTAS).header("Authorization", bearer).header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(UUID.randomUUID(), "1", "5", "10")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_SKU_NO_ENCONTRADO"));
        mockMvc.perform(post(VENTAS).header("Authorization", bearer).header("Idempotency-Key", "clave-1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(skuId, "0.5", "5", "10")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VEN_FRACCION_NO_PERMITIDA"));
    }

    @Test
    void deniesAccessWithoutThePermissionOrTheToken() throws Exception {
        mockMvc.perform(post(VENTAS).with(csrf()).header("Authorization", noPermissions)
                        .header("Idempotency-Key", "clave-1").contentType(MediaType.APPLICATION_JSON)
                        .content(ventaJson(skuId, "1", "5", "10")))
                .andExpect(status().isForbidden());
        mockMvc.perform(get(VENTAS).header("Authorization", noPermissions)).andExpect(status().isForbidden());
        mockMvc.perform(get(VENTAS)).andExpect(status().isUnauthorized());
    }

    @Test
    void annulsASaleRestoringTheStockToTheOriginalLotesAndReversingThePayment() throws Exception {
        var antiguo = ingresar("3", "L-ANT", LocalDate.now().plusMonths(6));
        var nuevo = ingresar("10", "L-NEW", LocalDate.now().plusYears(1));
        var cuerpo = vender("clave-anular", "5", "2.50", "20").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(post(ANULAR, ventaId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"  Error de cobro  \"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(ventaId.toString()))
                .andExpect(jsonPath("$.estado").value("ANULADA"))
                .andExpect(jsonPath("$.anulacion.motivo").value("Error de cobro"))
                .andExpect(jsonPath("$.anulacion.anuladaPorId").value(userId.toString()))
                .andExpect(jsonPath("$.anulacion.anuladaAt").exists());

        mockMvc.perform(get(INV + "/posiciones").header("Authorization", bearer)
                        .param("almacenId", almacenId.toString()).param("skuId", skuId.toString()))
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[?(@.loteId == '%s')].cantidadFisica", antiguo).value(3.0))
                .andExpect(jsonPath("$.items[?(@.loteId == '%s')].cantidadFisica", nuevo).value(10.0));
        var kardex = jdbcClient.sql("""
                        SELECT m.tipo_operacion_sunat, m.naturaleza, m.documento_tipo, m.actor
                          FROM sch_inventario.movimiento_inventario m
                          JOIN sch_admin.tenant t ON t.id = m.tenant_id
                         WHERE t.uuid_publico = :tenantId AND m.tipo_movimiento = 'ANULACION_VENTA'
                        """).param("tenantId", TENANT_ID).query().listOfRows();
        assertThat(kardex).hasSize(2).allSatisfy(fila -> assertThat(fila)
                .containsEntry("tipo_operacion_sunat", "05").containsEntry("naturaleza", "E")
                .containsEntry("documento_tipo", "ANULACION_VENTA").containsEntry("actor", userId.toString()));
        var fila = jdbcClient.sql("""
                        SELECT v.estado, v.motivo_anulacion, v.updated_by,
                               (SELECT p.estado FROM sch_venta.pago_venta p WHERE p.venta_id = v.id) AS pago_estado
                          FROM sch_venta.venta v WHERE v.uuid_publico = :ventaId
                        """).param("ventaId", ventaId).query().singleRow();
        assertThat(fila).containsEntry("estado", "ANULADA").containsEntry("pago_estado", "REVERSADO")
                .containsEntry("motivo_anulacion", "Error de cobro").containsEntry("updated_by", userId.toString());
    }

    @Test
    void anAnnulledSaleIsNotCountedByTheTurnoAndCannotBeAnnulledAgain() throws Exception {
        ingresar("20", "L-001", LocalDate.now().plusYears(1));
        var anulada = vender("clave-1", "2", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        vender("clave-2", "3", "5", "20").andExpect(status().isCreated());
        var ventaId = UUID.fromString(JsonPath.read(anulada, "$.id"));
        var anulacion = post(ANULAR, ventaId).header("Authorization", bearer)
                .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Cliente se arrepintio\"}");

        mockMvc.perform(anulacion).andExpect(status().isOk());
        mockMvc.perform(anulacion).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_VENTA_ESTADO_INVALIDO"));

        mockMvc.perform(get(VENTAS + "/{id}", ventaId).header("Authorization", bearer))
                .andExpect(jsonPath("$.estado").value("ANULADA"));
        var turnoId = UUID.fromString(JsonPath.read(mockMvc.perform(get(TURNOS + "/actual")
                        .header("Authorization", bearer).param("terminalId", terminalId.toString()))
                .andReturn().getResponse().getContentAsString(), "$.id"));
        mockMvc.perform(post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":115}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalVentasSistema").value(15.0))
                .andExpect(jsonPath("$.totalSistema").value(115.0))
                .andExpect(jsonPath("$.diferencia").value(0.0));
    }

    @Test
    void aSaleOfAClosedTurnoCannotBeAnnulled() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));
        var cuerpo = vender("clave-1", "1", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));
        var turnoId = UUID.fromString(JsonPath.read(mockMvc.perform(get(TURNOS + "/actual")
                        .header("Authorization", bearer).param("terminalId", terminalId.toString()))
                .andReturn().getResponse().getContentAsString(), "$.id"));
        mockMvc.perform(post(TURNOS + "/{id}/cierre", turnoId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"totalDeclarado\":105}"))
                .andExpect(status().isOk());

        mockMvc.perform(post(ANULAR, ventaId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Tarde\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("VEN_TURNO_NO_ABIERTO"));
    }

    @Test
    void validatesTheAnulacionRequestAndItsReferencesAndEnforcesThePermission() throws Exception {
        ingresar("10", "L-001", LocalDate.now().plusYears(1));
        var cuerpo = vender("clave-1", "1", "5", "10").andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        var ventaId = UUID.fromString(JsonPath.read(cuerpo, "$.id"));

        mockMvc.perform(post(ANULAR, ventaId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"   \"}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(ANULAR, ventaId).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest());
        mockMvc.perform(post(ANULAR, UUID.randomUUID()).header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEN_VENTA_NO_ENCONTRADA"));
        mockMvc.perform(post(ANULAR, ventaId).with(csrf()).header("Authorization", noPermissions)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isForbidden());
        mockMvc.perform(post(ANULAR, ventaId).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"motivo\":\"Motivo\"}"))
                .andExpect(status().isUnauthorized());
    }

    private void abrirTurno(UUID terminal) throws Exception {
        mockMvc.perform(post(TURNOS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"terminalId\":\"%s\",\"fondoInicial\":100}".formatted(terminal)))
                .andExpect(status().isCreated());
    }

    private ResultActions vender(String clave, String cantidad, String precio, String recibido) throws Exception {
        return vender(clave, ventaJson(terminalId, almacenId, skuId, cantidad, precio, recibido));
    }

    private ResultActions vender(UUID terminal, UUID almacen, String clave) throws Exception {
        return vender(clave, ventaJson(terminal, almacen, skuId, "1", "5", "10"));
    }

    private ResultActions vender(String clave, String json) throws Exception {
        var request = post(VENTAS).header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON)
                .content(json);
        return mockMvc.perform(clave == null ? request : request.header("Idempotency-Key", clave));
    }

    private String ventaJson(UUID sku, String cantidad, String precio, String recibido) {
        return ventaJson(terminalId, almacenId, sku, cantidad, precio, recibido);
    }

    private static String ventaJson(
            UUID terminal, UUID almacen, UUID sku, String cantidad, String precio, String recibido) {
        return """
                {"terminalId":"%s","almacenId":"%s",
                 "lineas":[{"skuId":"%s","cantidad":%s,"precioUnitario":%s}],
                 "pago":{"montoRecibido":%s}}
                """.formatted(terminal, almacen, sku, cantidad, precio, recibido);
    }

    private UUID ingresar(String cantidad, String numeroLote, LocalDate vencimiento) throws Exception {
        return ingresar(almacenId, cantidad, numeroLote, vencimiento);
    }

    private UUID ingresar(UUID almacen, String cantidad, String numeroLote, LocalDate vencimiento)
            throws Exception {
        var cuerpo = mockMvc.perform(post(INV + "/movimientos").header("Authorization", bearer)
                        .contentType(MediaType.APPLICATION_JSON).content("""
                                {"almacenId":"%s","skuId":"%s","tipo":"AJUSTE_INGRESO","cantidad":%s,
                                 "motivo":"Saldo inicial","numeroLote":"%s","fechaVencimiento":"%s"}
                                """.formatted(almacen, skuId, cantidad, numeroLote, vencimiento)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(cuerpo, "$.loteId"));
    }

    private UUID created(MockHttpServletRequestBuilder request) throws Exception {
        var body = mockMvc.perform(request.header("Authorization", bearer).contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return UUID.fromString(JsonPath.read(body, "$.id"));
    }
}
